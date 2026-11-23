package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import io.github.nicolasgoyon.beaconhold.network.AreaSyncPayload;
import io.github.nicolasgoyon.beaconhold.network.AreaVisibility;
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

    /** A beacon with an active beam and no function shows the dome of its area. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_dome_beam")
    public static void beaconWithNoFunctionHasADome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 2);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hasArea(level, beacon, 38), "The dome must have a radius of 38"))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE))
                .thenWaitUntil(() -> helper.assertFalse(hasAnyArea(level, beacon), "A blocked beam has no dome"))
                .thenExecute(() -> BeaconTestSupport.removeBeacon(helper))
                .thenSucceed();
    }

    /** With the vanilla powers in the vanilla range, a beacon with no function shows no dome. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_dome_beam_vanilla")
    public static void beaconWithNoFunctionHasNoDomeWithTheVanillaRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.SERVER.setVanillaPowersUseArea(false);
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        helper.startSequence()
                .thenExecuteAfter((int) BeaconTracker.UPDATE_INTERVAL * 3, () -> {
                    boolean dome = hasAnyArea(level, beacon);
                    BeaconholdConfig.SERVER.setVanillaPowersUseArea(true);
                    helper.assertFalse(dome, "No dome with the vanilla range");
                })
                .thenWaitUntil(() -> helper.assertTrue(hasArea(level, beacon, 20), "The option on gives the dome again"))
                .thenExecute(() -> BeaconTestSupport.removeBeacon(helper))
                .thenSucceed();
    }

    /** The server sends an area only to the players near its edge. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_dome_near")
    public static void areaGoesOnlyToNearPlayers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        AreaSyncPayload.Area area = new AreaSyncPayload.Area(beacon, 20);
        double x = beacon.getX() + 0.5;
        double z = beacon.getZ() + 0.5;
        double limit = 20 + AreaVisibility.SEND_DISTANCE;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(hasArea(level, beacon, 20), "The beacon must be active"))
                .thenExecute(() -> {
                    helper.assertTrue(AreaSync.areasNear(level, x, z).contains(area), "A player at the beacon");
                    helper.assertTrue(
                            AreaSync.areasNear(level, x + limit - 1, z).contains(area), "A player near the limit");
                    helper.assertFalse(
                            AreaSync.areasNear(level, x + limit + 1, z).contains(area), "A player out of the limit");
                    helper.assertFalse(
                            AreaSync.areasNear(level, x, z - limit - 1).contains(area), "A player out of the limit");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }
}
