package io.github.nicolasgoyon.beaconhold.function;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconRecord;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconSavedData;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The function "loaded chunks": an active beacon keeps the chunks near it loaded and ticking.
 *
 * <p>NeoForge saves the tickets, so the chunks stay loaded after a restart of the server. When the server loads a
 * world, it removes the tickets that the saved list of the chunks does not contain. When the server starts, it makes the
 * tickets agree with the saved lists (see {@link #restore(ServerLevel)}), then applies the current options (see
 * {@link #rebalance(MinecraftServer)}).
 */
public final class LoadedChunks {
    /** The controller of the chunk tickets of the beacons. */
    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(BeaconholdApi.MOD_ID, "beacon"), LoadedChunks::validateTickets);

    /** True when a beacon released chunks in this tick. The server then gives the free capacity at the end of the tick. */
    private static boolean rebalancePending;

    private LoadedChunks() {
    }

    /**
     * Registers the controller of the tickets and the listeners of the beacon events.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterTicketControllersEvent.class, event -> event.register(CONTROLLER));
        NeoForge.EVENT_BUS.addListener(BeaconFunctionEvent.Start.class, LoadedChunks::onStart);
        NeoForge.EVENT_BUS.addListener(BeaconFunctionEvent.Stop.class, LoadedChunks::onStop);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (rebalancePending) {
                rebalancePending = false;
                rebalance(event.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                restore(level);
            }
            rebalance(event.getServer());
        });
    }

    /**
     * Makes the tickets of a dimension agree with the saved lists. The server calls this method when it starts.
     *
     * <ul>
     *   <li>A beacon that is active with the function "loaded chunks" in the saved list of the beacons gets a ticket for
     *       each of its saved chunks.
     *   <li>The other beacons of the saved list of the chunks release their chunks.
     * </ul>
     *
     * @param level the dimension
     */
    public static void restore(ServerLevel level) {
        LoadedChunksSavedData data = LoadedChunksSavedData.get(level);
        BeaconSavedData beacons = BeaconSavedData.get(level);
        for (BlockPos pos : data.positions()) {
            if (!ChunkRules.keepsChunks(beacons.get(pos), ModFunctions.LOADED_CHUNKS.getId())) {
                release(level, pos);
                continue;
            }
            LongSet chunks = data.get(pos);
            if (chunks != null) {
                for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
                    ChunkPos chunk = new ChunkPos(it.nextLong());
                    CONTROLLER.forceChunk(level, pos, chunk.x, chunk.z, true, true);
                }
            }
        }
    }

    private static boolean isLoadedChunks(ActiveBeacon beacon) {
        return beacon.function().equals(ModFunctions.LOADED_CHUNKS.getKey());
    }

    private static void onStart(BeaconFunctionEvent.Start event) {
        if (isLoadedChunks(event.getBeacon())) {
            load(event.getLevel(), event.getBeacon());
        }
    }

    private static void onStop(BeaconFunctionEvent.Stop event) {
        if (isLoadedChunks(event.getBeacon()) && release(event.getLevel(), event.getBeacon().pos())) {
            rebalancePending = true;
        }
    }

    private static void load(ServerLevel level, ActiveBeacon beacon) {
        release(level, beacon.pos());
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        List<ChunkPos> wanted = ChunkRules.areaChunks(
                beacon.pos().getX(), beacon.pos().getZ(), beacon.area().radius());
        UUID owner = ownerOf(level, beacon.pos());
        int allowed = ChunkRules.allowedChunks(
                wanted.size(),
                totalLoadedChunks(level),
                config.maxLoadedChunks(),
                owner != null,
                owner == null ? 0 : ownerChunks(level, owner),
                config.maxChunksPerPlayer());
        LongSet chunks = new LongOpenHashSet();
        for (ChunkPos chunk : wanted.subList(0, allowed)) {
            CONTROLLER.forceChunk(level, beacon.pos(), chunk.x, chunk.z, true, true);
            chunks.add(chunk.toLong());
        }
        if (!chunks.isEmpty()) {
            LoadedChunksSavedData.get(level).set(beacon.pos(), chunks);
        }
    }

    /** Releases the chunks of a beacon, and tells if the beacon had chunks. */
    private static boolean release(ServerLevel level, BlockPos pos) {
        LoadedChunksSavedData data = LoadedChunksSavedData.get(level);
        LongSet chunks = data.get(pos);
        if (chunks == null) {
            return false;
        }
        for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
            ChunkPos chunk = new ChunkPos(it.nextLong());
            CONTROLLER.forceChunk(level, pos, chunk.x, chunk.z, false, true);
        }
        data.set(pos, null);
        return true;
    }

    /**
     * Gives the chunks to each active beacon with this function again, with the current options. The server calls this
     * method after a change of the options, and at the end of a tick when a beacon released chunks. Thus the beacons
     * that got fewer chunks because of the limit get the free capacity.
     *
     * <p>The beacons get their chunks in the order of the dimensions, then in the order of the saved list of the
     * chunks. A beacon with no saved chunks comes after the beacons with saved chunks. The method changes only the
     * tickets of the chunks that a beacon gets or releases.
     *
     * <p>A beacon with a known owner also has the limit for each player. A beacon with no known owner has only the
     * limit of the server.
     *
     * @param server the server
     */
    public static void rebalance(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        List<ActiveBeacon> beacons = new ArrayList<>();
        List<UUID> owners = new ArrayList<>();
        List<List<ChunkPos>> areas = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            LoadedChunksSavedData data = LoadedChunksSavedData.get(level);
            Map<BlockPos, ActiveBeacon> active = new LinkedHashMap<>();
            for (ActiveBeacon beacon : BeaconholdApi.activeBeacons(level)) {
                if (isLoadedChunks(beacon)) {
                    active.put(beacon.pos(), beacon);
                }
            }
            for (BlockPos pos : data.positions()) {
                if (!active.containsKey(pos)) {
                    release(level, pos);
                }
            }
            List<ActiveBeacon> ordered = new ArrayList<>();
            for (BlockPos pos : data.positions()) {
                ordered.add(active.remove(pos));
            }
            ordered.addAll(active.values());
            for (ActiveBeacon beacon : ordered) {
                levels.add(level);
                beacons.add(beacon);
                owners.add(ownerOf(level, beacon.pos()));
                areas.add(ChunkRules.areaChunks(
                        beacon.pos().getX(), beacon.pos().getZ(), beacon.area().radius()));
            }
        }
        int[] wanted = new int[areas.size()];
        for (int i = 0; i < wanted.length; i++) {
            wanted[i] = areas.get(i).size();
        }
        int[] allowed = ChunkRules.allocate(
                wanted,
                owners,
                BeaconholdConfig.SERVER.maxLoadedChunks(),
                BeaconholdConfig.SERVER.maxChunksPerPlayer());
        for (int i = 0; i < allowed.length; i++) {
            LongSet chunks = new LongOpenHashSet();
            for (ChunkPos chunk : areas.get(i).subList(0, allowed[i])) {
                chunks.add(chunk.toLong());
            }
            assign(levels.get(i), beacons.get(i).pos(), chunks);
        }
    }

    /** Sets the chunks of a beacon. The method changes only the tickets of the chunks that are new or released. */
    private static void assign(ServerLevel level, BlockPos pos, LongSet chunks) {
        LoadedChunksSavedData data = LoadedChunksSavedData.get(level);
        LongSet before = data.get(pos);
        if (before != null) {
            for (LongIterator it = before.iterator(); it.hasNext(); ) {
                long chunk = it.nextLong();
                if (!chunks.contains(chunk)) {
                    CONTROLLER.forceChunk(level, pos, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                }
            }
        }
        for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
            long chunk = it.nextLong();
            if (before == null || !before.contains(chunk)) {
                CONTROLLER.forceChunk(level, pos, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
            }
        }
        data.set(pos, chunks.isEmpty() ? null : chunks);
    }

    /**
     * Gives the number of chunks that all the beacons of the server load.
     *
     * @param level a dimension of the server
     * @return the sum for all the dimensions
     */
    public static int totalLoadedChunks(ServerLevel level) {
        int total = 0;
        for (ServerLevel dimension : level.getServer().getAllLevels()) {
            total += LoadedChunksSavedData.get(dimension).total();
        }
        return total;
    }

    /**
     * Gives the owner of a beacon from the saved list of the beacons.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @return the UUID of the player that placed the beacon, or {@code null} if no player is known
     */
    @Nullable
    public static UUID ownerOf(ServerLevel level, BlockPos pos) {
        BeaconRecord beacon = BeaconSavedData.get(level).get(pos);
        return beacon == null ? null : beacon.owner();
    }

    /**
     * Gives the number of chunks that all the beacons of a player load.
     *
     * @param level a dimension of the server
     * @param owner the UUID of the player
     * @return the sum for all the dimensions
     */
    public static int ownerChunks(ServerLevel level, UUID owner) {
        int total = 0;
        for (ServerLevel dimension : level.getServer().getAllLevels()) {
            LoadedChunksSavedData data = LoadedChunksSavedData.get(dimension);
            for (BlockPos pos : data.positions()) {
                LongSet chunks = data.get(pos);
                if (chunks != null && Objects.equals(owner, ownerOf(dimension, pos))) {
                    total += chunks.size();
                }
            }
        }
        return total;
    }

    /**
     * Gives the number of chunks that the area of an active beacon with this function touches.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @return the number of chunks, or 0 if the beacon is not active with this function
     */
    public static int areaChunkCount(ServerLevel level, BlockPos pos) {
        for (ActiveBeacon beacon : BeaconholdApi.beaconsCovering(level, pos, ModFunctions.LOADED_CHUNKS.getKey())) {
            if (beacon.pos().equals(pos)) {
                return ChunkRules.areaChunks(pos.getX(), pos.getZ(), beacon.area().radius()).size();
            }
        }
        return 0;
    }

    /**
     * Gives the chunks that a beacon loads.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @return a copy of the chunks. The set is empty when the beacon loads no chunk.
     */
    public static LongSet chunksOf(ServerLevel level, BlockPos pos) {
        LongSet chunks = LoadedChunksSavedData.get(level).get(pos);
        return chunks == null ? new LongOpenHashSet() : new LongOpenHashSet(chunks);
    }

    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        LoadedChunksSavedData data = LoadedChunksSavedData.get(level);
        for (Map.Entry<BlockPos, TicketSet> entry : List.copyOf(helper.getBlockTickets().entrySet())) {
            BlockPos owner = entry.getKey();
            LongSet saved = data.get(owner);
            removeTickets(helper, owner, ChunkRules.invalidChunks(entry.getValue().ticking(), saved), true);
            removeTickets(helper, owner, ChunkRules.invalidChunks(entry.getValue().nonTicking(), null), false);
        }
    }

    private static void removeTickets(TicketHelper helper, BlockPos owner, LongSet chunks, boolean ticking) {
        for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
            helper.removeTicket(owner, it.nextLong(), ticking);
        }
    }
}
