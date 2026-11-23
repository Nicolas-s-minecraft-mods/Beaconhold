package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.config.ServerOptionChanges;
import io.github.nicolasgoyon.beaconhold.function.ChunkRules;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import io.github.nicolasgoyon.beaconhold.network.AreaSyncPayload;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of a change of the server options while the beacons are active. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OptionsGameTests {
    private static final int TIMEOUT = 300;

    /** A radius for the pyramid level 1 that is different from the default radius. */
    private static final int NEW_RADIUS = 40;

    private OptionsGameTests() {
    }

    private static BlockPos activeBeacon(GameTestHelper helper) {
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(helper.getLevel(), beacon, ModFunctions.LOADED_CHUNKS.getKey());
        return beacon;
    }

    private static boolean isActive(ServerLevel level, BlockPos beacon) {
        return BeaconholdApi.isCovered(level, beacon, ModFunctions.LOADED_CHUNKS.getKey());
    }

    private static void assertChunksOfRadius(GameTestHelper helper, BlockPos beacon, int radius) {
        List<ChunkPos> area = ChunkRules.areaChunks(beacon.getX(), beacon.getZ(), radius);
        LongSet chunks = LoadedChunks.chunksOf(helper.getLevel(), beacon);
        helper.assertTrue(
                chunks.size() == area.size(),
                "Radius " + radius + ": expected " + area.size() + " chunks, got " + chunks.size());
        for (ChunkPos chunk : area) {
            helper.assertTrue(chunks.contains(chunk.toLong()), "The beacon must load each chunk of its area");
        }
    }

    /** A new radius changes the area, the loaded chunks and the dome of an active beacon at once. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_options_radius")
    public static void newRadiusChangesTheActiveBeacons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int before = config.radius(1);
        BlockPos beacon = activeBeacon(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(isActive(level, beacon), "The beacon must be active"))
                .thenExecute(() -> {
                    assertChunksOfRadius(helper, beacon, before);
                    BlockPos far = beacon.offset(NEW_RADIUS - 2, 0, 0);
                    helper.assertFalse(
                            BeaconholdApi.isCovered(level, far, ModFunctions.LOADED_CHUNKS.getKey()),
                            "The default area must not cover the far position");
                    try {
                        config.setRadius(1, NEW_RADIUS);
                        ServerOptionChanges.apply(level.getServer());
                        assertChunksOfRadius(helper, beacon, NEW_RADIUS);
                        helper.assertTrue(
                                BeaconholdApi.isCovered(level, far, ModFunctions.LOADED_CHUNKS.getKey()),
                                "The new area must cover the far position");
                        helper.assertTrue(
                                AreaSync.areas(level).contains(new AreaSyncPayload.Area(beacon, NEW_RADIUS)),
                                "The dome must use the new radius");
                        helper.assertTrue(AreaSync.isChanged(level), "The server must send the areas again");
                    } finally {
                        config.setRadius(1, before);
                        ServerOptionChanges.apply(level.getServer());
                    }
                    assertChunksOfRadius(helper, beacon, before);
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A lower limit of the server reduces the chunks of an active beacon at once. A higher limit gives them back. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_options_limit")
    public static void newLimitChangesTheActiveBeacons(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int before = config.maxLoadedChunks();
        BlockPos beacon = activeBeacon(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(isActive(level, beacon), "The beacon must be active"))
                .thenExecute(() -> {
                    assertChunksOfRadius(helper, beacon, config.radius(1));
                    int own = LoadedChunks.chunksOf(level, beacon).size();
                    int others = LoadedChunks.totalLoadedChunks(level) - own;
                    try {
                        config.setMaxLoadedChunks(others + 2);
                        ServerOptionChanges.apply(level.getServer());
                        LongSet chunks = LoadedChunks.chunksOf(level, beacon);
                        helper.assertTrue(chunks.size() == 2, "Expected 2 chunks, got " + chunks.size());
                        helper.assertTrue(
                                chunks.contains(new ChunkPos(beacon).toLong()), "The beacon keeps its own chunk");
                        helper.assertTrue(isActive(level, beacon), "The beacon stays active");
                    } finally {
                        config.setMaxLoadedChunks(before);
                        ServerOptionChanges.apply(level.getServer());
                    }
                    assertChunksOfRadius(helper, beacon, config.radius(1));
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }
}
