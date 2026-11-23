package io.github.nicolasgoyon.beaconhold.function;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconSavedData;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
