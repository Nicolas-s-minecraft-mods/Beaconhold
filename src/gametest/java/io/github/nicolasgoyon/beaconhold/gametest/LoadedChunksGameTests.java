package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of the function "loaded chunks". */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LoadedChunksGameTests {
    private static final int TIMEOUT = 300;

    private LoadedChunksGameTests() {
    }

    private static BlockPos activeBeacon(GameTestHelper helper, int pyramidLevel) {
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, pyramidLevel);
        BeaconholdApi.setFunction(helper.getLevel(), beacon, ModFunctions.LOADED_CHUNKS.getKey());
        return beacon;
    }

    /** Gives the chunks with a ticking ticket of a block in the NeoForge saved data. */
    private static LongSet forcedTickingChunks(ServerLevel level) {
        LongSet result = new LongOpenHashSet();
        ForcedChunksSavedData data = level.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        if (data != null) {
            data.getBlockForcedChunks().getTickingChunks().values().forEach(result::addAll);
        }
        return result;
    }

    private static void assertChunks(GameTestHelper helper, BlockPos beacon, int expected) {
        LongSet chunks = LoadedChunks.chunksOf(helper.getLevel(), beacon);
        helper.assertTrue(chunks.size() == expected, "Expected " + expected + " chunks, got " + chunks.size());
        helper.assertTrue(chunks.contains(new ChunkPos(beacon).toLong()), "The chunk of the beacon must be loaded");
        helper.assertTrue(
                forcedTickingChunks(helper.getLevel()).containsAll(chunks), "NeoForge must have a ticket for each chunk");
    }

    private static void assertNoChunk(GameTestHelper helper, BlockPos beacon, LongSet before) {
        helper.assertTrue(LoadedChunks.chunksOf(helper.getLevel(), beacon).isEmpty(), "The beacon must load no chunk");
        LongSet forced = forcedTickingChunks(helper.getLevel());
        for (long chunk : before) {
            helper.assertFalse(forced.contains(chunk), "NeoForge must have no ticket for the chunks of the beacon");
        }
    }

    /** A larger pyramid loads more chunks. The chunks stay loaded, with a ticket of NeoForge. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_level")
    public static void largerPyramidLoadsMoreChunks(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper, 1);
        helper.startSequence()
                .thenWaitUntil(() -> assertChunks(helper, beacon, 1))
                .thenExecute(() -> BeaconTestSupport.addLayer(helper, 2))
                .thenWaitUntil(() -> assertChunks(helper, beacon, 5))
                .thenExecute(() -> {
                    LongSet chunks = LoadedChunks.chunksOf(helper.getLevel(), beacon);
                    BeaconTestSupport.removeBeacon(helper);
                    assertNoChunk(helper, beacon, chunks);
                })
                .thenSucceed();
    }

    /** A beacon releases its chunks when a block stops its beam. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_blocked")
    public static void blockedBeamReleasesTheChunks(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper, 2);
        LongSet chunks = new LongOpenHashSet();
        helper.startSequence()
                .thenWaitUntil(() -> assertChunks(helper, beacon, 5))
                .thenExecute(() -> {
                    chunks.addAll(LoadedChunks.chunksOf(helper.getLevel(), beacon));
                    helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.GLASS);
                })
                .thenIdle(100)
                .thenExecute(() -> assertChunks(helper, beacon, 5))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE))
                .thenWaitUntil(() -> assertNoChunk(helper, beacon, chunks))
                .thenExecute(() -> BeaconTestSupport.removeBeacon(helper))
                .thenSucceed();
    }

    /** A removed beacon releases its chunks. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_removed")
    public static void removedBeaconReleasesTheChunks(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper, 1);
        LongSet chunks = new LongOpenHashSet();
        helper.startSequence()
                .thenWaitUntil(() -> assertChunks(helper, beacon, 1))
                .thenExecute(() -> {
                    chunks.addAll(LoadedChunks.chunksOf(helper.getLevel(), beacon));
                    helper.setBlock(BeaconTestSupport.BEACON, Blocks.AIR);
                })
                .thenWaitUntil(() -> assertNoChunk(helper, beacon, chunks))
                .thenSucceed();
    }

    /** The limit of the server reduces the chunks of a beacon. The beacon keeps its nearest chunks. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_limit")
    public static void limitOfTheServerReducesTheChunks(GameTestHelper helper) {
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int before = config.maxLoadedChunks();
        config.setMaxLoadedChunks(LoadedChunks.totalLoadedChunks(helper.getLevel()) + 3);
        BlockPos beacon = activeBeacon(helper, 2);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(helper.getLevel(), beacon, ModFunctions.LOADED_CHUNKS.getKey()),
                        "The beacon must be active"))
                .thenExecute(() -> {
                    config.setMaxLoadedChunks(before);
                    assertChunks(helper, beacon, 3);
                    LongSet chunks = LoadedChunks.chunksOf(helper.getLevel(), beacon);
                    ChunkPos center = new ChunkPos(beacon);
                    for (long chunk : chunks) {
                        ChunkPos pos = new ChunkPos(chunk);
                        helper.assertTrue(
                                Math.abs(pos.x - center.x) + Math.abs(pos.z - center.z) <= 1,
                                "The beacon must keep its nearest chunks");
                    }
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon with another function loads no chunk. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_other")
    public static void otherFunctionLoadsNoChunk(GameTestHelper helper) {
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        ServerLevel level = helper.getLevel();
        BeaconholdApi.setFunction(level, beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey()),
                        "The beacon must be active"))
                .thenExecute(() -> {
                    helper.assertTrue(LoadedChunks.chunksOf(level, beacon).isEmpty(), "No chunk");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }
}
