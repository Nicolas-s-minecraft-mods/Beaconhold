package io.github.nicolasgoyon.beaconhold.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import io.github.nicolasgoyon.beaconhold.function.ChunkRules;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunksSavedData;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
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

    /**
     * Checks that a beacon loads the chunks that its area touches. A beacon at the level 1 loads 9 to 13 chunks, and a
     * beacon at the level 2 loads 36 to 41 chunks.
     */
    private static void assertAreaChunks(GameTestHelper helper, BlockPos beacon, int pyramidLevel) {
        int radius = BeaconholdConfig.SERVER.radius(pyramidLevel);
        List<ChunkPos> area = ChunkRules.areaChunks(beacon.getX(), beacon.getZ(), radius);
        int min = pyramidLevel == 1 ? 9 : 36;
        int max = pyramidLevel == 1 ? 13 : 41;
        helper.assertTrue(
                area.size() >= min && area.size() <= max, "The area touches " + area.size() + " chunks");
        assertChunks(helper, beacon, area.size());
        LongSet chunks = LoadedChunks.chunksOf(helper.getLevel(), beacon);
        for (ChunkPos chunk : area) {
            helper.assertTrue(chunks.contains(chunk.toLong()), "The beacon must load each chunk of its area");
        }
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

    /** A larger pyramid loads more chunks: the chunks of its larger area. The chunks stay loaded, with a ticket of NeoForge. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_level")
    public static void largerPyramidLoadsMoreChunks(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper, 1);
        helper.startSequence()
                .thenWaitUntil(() -> assertAreaChunks(helper, beacon, 1))
                .thenExecute(() -> BeaconTestSupport.addLayer(helper, 2))
                .thenWaitUntil(() -> assertAreaChunks(helper, beacon, 2))
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
                .thenWaitUntil(() -> assertAreaChunks(helper, beacon, 2))
                .thenExecute(() -> {
                    chunks.addAll(LoadedChunks.chunksOf(helper.getLevel(), beacon));
                    helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.GLASS);
                })
                .thenIdle(100)
                .thenExecute(() -> assertAreaChunks(helper, beacon, 2))
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
                .thenWaitUntil(() -> assertAreaChunks(helper, beacon, 1))
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

    /**
     * At the start of the server, an active beacon gets a ticket again for each chunk of the saved list. The test removes
     * the tickets of NeoForge and keeps the saved list, as after a stop of the server between the two saves.
     */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_restore")
    public static void restoreGivesBackTheMissingTickets(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper, 1);
        ServerLevel level = helper.getLevel();
        LongSet chunks = new LongOpenHashSet();
        helper.startSequence()
                .thenWaitUntil(() -> assertAreaChunks(helper, beacon, 1))
                .thenExecute(() -> {
                    chunks.addAll(LoadedChunks.chunksOf(level, beacon));
                    for (long chunk : chunks) {
                        ChunkPos pos = new ChunkPos(chunk);
                        LoadedChunks.CONTROLLER.forceChunk(level, beacon, pos.x, pos.z, false, true);
                    }
                    helper.assertFalse(
                            forcedTickingChunks(level).contains(new ChunkPos(beacon).toLong()),
                            "The test must remove the tickets");
                    LoadedChunks.restore(level);
                    assertAreaChunks(helper, beacon, 1);
                    BeaconTestSupport.removeBeacon(helper);
                    assertNoChunk(helper, beacon, chunks);
                })
                .thenSucceed();
    }

    /** At the start of the server, a saved position with no active beacon releases its chunks and its tickets. */
    @GameTest(template = "empty", batch = "beaconhold_chunks_restore_stale")
    public static void restoreReleasesTheChunksOfAMissingBeacon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos missing = helper.absolutePos(BlockPos.ZERO).offset(4096, 0, 0);
        ChunkPos chunk = new ChunkPos(missing);
        LoadedChunks.CONTROLLER.forceChunk(level, missing, chunk.x, chunk.z, true, true);
        LoadedChunksSavedData.get(level).set(missing, new LongOpenHashSet(new long[] {chunk.toLong()}));
        helper.assertTrue(forcedTickingChunks(level).contains(chunk.toLong()), "The test must add a ticket");
        LoadedChunks.restore(level);
        helper.assertTrue(LoadedChunks.chunksOf(level, missing).isEmpty(), "The saved list must have no chunk");
        helper.assertFalse(forcedTickingChunks(level).contains(chunk.toLong()), "NeoForge must have no ticket");
        helper.succeed();
    }

    /** Builds a beacon at the pyramid level 1 with a stock, at a relative position of the template "two_beacons". */
    private static BlockPos buildBeaconAt(GameTestHelper helper, BlockPos relative) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(relative.offset(x, -1, z), Blocks.IRON_BLOCK);
            }
        }
        helper.setBlock(relative, Blocks.BEACON);
        BeaconBlockEntity entity = helper.getBlockEntity(relative);
        BeaconStock.setStock(entity, new ItemStack(Items.AMETHYST_SHARD, 64));
        return helper.absolutePos(relative);
    }

    private static void removeBeaconAt(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        BeaconholdApi.setFunction(helper.getLevel(), pos, null);
        if (helper.getLevel().getBlockEntity(pos) instanceof BeaconBlockEntity entity) {
            BeaconStock.setStock(entity, ItemStack.EMPTY);
        }
        helper.setBlock(relative, Blocks.AIR);
    }

    /**
     * A beacon that got fewer chunks because of the limit gets the free capacity when another beacon releases its
     * chunks.
     */
    @GameTest(template = "two_beacons", timeoutTicks = TIMEOUT, batch = "beaconhold_chunks_free")
    public static void releasedChunksGoToTheBeaconWithFewerChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int before = config.maxLoadedChunks();
        BlockPos firstRelative = new BlockPos(2, 2, 2);
        BlockPos secondRelative = new BlockPos(10, 2, 2);
        BlockPos first = buildBeaconAt(helper, firstRelative);
        BlockPos second = buildBeaconAt(helper, secondRelative);
        int secondArea = ChunkRules.areaChunks(second.getX(), second.getZ(), config.radius(1)).size();
        BeaconholdApi.setFunction(level, first, ModFunctions.LOADED_CHUNKS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, first, ModFunctions.LOADED_CHUNKS.getKey()),
                        "The first beacon must be active"))
                .thenExecute(() -> {
                    config.setMaxLoadedChunks(LoadedChunks.totalLoadedChunks(level) + 3);
                    BeaconholdApi.setFunction(level, second, ModFunctions.LOADED_CHUNKS.getKey());
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, second, ModFunctions.LOADED_CHUNKS.getKey()),
                        "The second beacon must be active"))
                .thenExecute(() -> {
                    int chunks = LoadedChunks.chunksOf(level, second).size();
                    if (chunks != 3) {
                        config.setMaxLoadedChunks(before);
                    }
                    helper.assertTrue(chunks == 3, "The limit gives 3 chunks to the second beacon, got " + chunks);
                    removeBeaconAt(helper, firstRelative);
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        LoadedChunks.chunksOf(level, second).size() == secondArea,
                        "The second beacon must get the chunks of its full area"))
                .thenExecute(() -> {
                    config.setMaxLoadedChunks(before);
                    helper.assertTrue(
                            forcedTickingChunks(level).containsAll(LoadedChunks.chunksOf(level, second)),
                            "NeoForge must have a ticket for each chunk");
                    removeBeaconAt(helper, secondRelative);
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

    /** The command of the operators shows and sets the limit of the server. */
    @GameTest(template = "beacon", batch = "beaconhold_chunks_command")
    public static void commandSetsTheLimit(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        Commands commands = server.getCommands();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int before = config.maxLoadedChunks();
        try {
            int shown = commands.getDispatcher().execute("beaconhold chunks limit", source);
            helper.assertTrue(shown == before, "The command shows the limit");
            commands.getDispatcher().execute("beaconhold chunks limit 300", source);
            helper.assertTrue(config.maxLoadedChunks() == 300, "The command sets the limit");
            try {
                commands.getDispatcher().execute("beaconhold chunks limit -1", source);
                helper.fail("The command refuses a negative limit");
            } catch (CommandSyntaxException expected) {
                helper.assertTrue(config.maxLoadedChunks() == 300, "The limit does not change");
            }
        } catch (CommandSyntaxException exception) {
            helper.fail("The command must operate: " + exception.getMessage());
        } finally {
            config.setMaxLoadedChunks(before);
            BeaconholdConfig.SERVER_SPEC.save();
        }
        helper.succeed();
    }
}
