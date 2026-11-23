package io.github.nicolasgoyon.beaconhold.function;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconSavedData;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/**
 * The function "loaded chunks": an active beacon keeps the chunks near it loaded and ticking.
 *
 * <p>NeoForge saves the tickets, so the chunks stay loaded after a restart of the server. When the server loads a
 * world, it removes the tickets that the saved list of the chunks does not contain. When the server starts, it makes the
 * tickets agree with the saved lists (see {@link #restore(ServerLevel)}).
 */
public final class LoadedChunks {
    /** The controller of the chunk tickets of the beacons. */
    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(BeaconholdApi.MOD_ID, "beacon"), LoadedChunks::validateTickets);

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
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                restore(level);
            }
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
        if (isLoadedChunks(event.getBeacon())) {
            release(event.getLevel(), event.getBeacon().pos());
        }
    }

    private static void load(ServerLevel level, ActiveBeacon beacon) {
        release(level, beacon.pos());
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        List<ChunkPos> wanted = ChunkRules.areaChunks(
                beacon.pos().getX(), beacon.pos().getZ(), beacon.area().radius());
        int allowed = ChunkRules.allowedChunks(wanted.size(), totalLoadedChunks(level), config.maxLoadedChunks());
        LongSet chunks = new LongOpenHashSet();
        for (ChunkPos chunk : wanted.subList(0, allowed)) {
            CONTROLLER.forceChunk(level, beacon.pos(), chunk.x, chunk.z, true, true);
            chunks.add(chunk.toLong());
        }
        if (!chunks.isEmpty()) {
            LoadedChunksSavedData.get(level).set(beacon.pos(), chunks);
        }
    }

    private static void release(ServerLevel level, BlockPos pos) {
        LoadedChunksSavedData data = LoadedChunksSavedData.get(level);
        LongSet chunks = data.get(pos);
        if (chunks == null) {
            return;
        }
        for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
            ChunkPos chunk = new ChunkPos(it.nextLong());
            CONTROLLER.forceChunk(level, pos, chunk.x, chunk.z, false, true);
        }
        data.set(pos, null);
    }

    /**
     * Gives the chunks to each active beacon with this function again, with the current options. The server calls this
     * method after a change of the options.
     *
     * <p>The beacons get their chunks in the order of the dimensions, then in the order of the saved list of the
     * chunks. A beacon with no saved chunks comes after the beacons with saved chunks. The method changes only the
     * tickets of the chunks that a beacon gets or releases.
     *
     * @param server the server
     */
    public static void rebalance(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        List<ActiveBeacon> beacons = new ArrayList<>();
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
                areas.add(ChunkRules.areaChunks(
                        beacon.pos().getX(), beacon.pos().getZ(), beacon.area().radius()));
            }
        }
        int[] wanted = new int[areas.size()];
        for (int i = 0; i < wanted.length; i++) {
            wanted[i] = areas.get(i).size();
        }
        int[] allowed = ChunkRules.allocate(wanted, BeaconholdConfig.SERVER.maxLoadedChunks());
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
