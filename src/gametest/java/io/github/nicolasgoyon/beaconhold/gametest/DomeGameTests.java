package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import io.github.nicolasgoyon.beaconhold.network.AreaSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of the areas that the server sends to the clients for the energy domes. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DomeGameTests {
    private static final int TIMEOUT = 300;

    private DomeGameTests() {
    }

    private static boolean hasArea(ServerLevel level, BlockPos beacon, int radius) {
        return AreaSync.areas(level).contains(new AreaSyncPayload.Area(beacon, radius));
    }

    private static boolean hasAnyArea(ServerLevel level, BlockPos beacon) {
        return AreaSync.areas(level).stream().anyMatch(area -> area.center().equals(beacon));
    }

    /** An active beacon has a dome with the radius of its area. A blocked beam removes the dome. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_dome_active")
    public static void activeBeaconHasADome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hasArea(level, beacon, 20), "The dome must have a radius of 20"))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE))
                .thenWaitUntil(() -> helper.assertFalse(hasAnyArea(level, beacon), "A blocked beam has no dome"))
                .thenExecute(() -> BeaconTestSupport.removeBeacon(helper))
                .thenSucceed();
    }

    /** The function "loaded chunks" uses the area of the beacon, so it has a dome. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_dome_chunks")
    public static void loadedChunksHasADome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, ModFunctions.LOADED_CHUNKS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hasArea(level, beacon, 20), "The dome must have a radius of 20"))
                .thenExecute(() -> BeaconTestSupport.removeBeacon(helper))
                .thenSucceed();
    }

    /** A change of an active function marks the dimension, so that the server sends the areas. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_dome_mark")
    public static void startMarksTheDimension(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hasArea(level, beacon, 20), "The beacon must be active"))
                .thenExecute(() -> {
                    BeaconholdApi.setFunction(level, beacon, TestFunctions.NO_AREA);
                    helper.assertTrue(AreaSync.isChanged(level), "A new function must mark the dimension");
                    helper.assertFalse(hasAnyArea(level, beacon), "A function with no area: no dome");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }
}
