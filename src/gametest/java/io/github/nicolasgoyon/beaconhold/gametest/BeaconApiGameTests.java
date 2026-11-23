package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.config.ServerOptionChanges;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game tests of the API: the state of a beacon, the queries of the areas and the events. Each test has its own
 * batch, so that the area of one beacon does not cover the positions of another test.
 */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BeaconApiGameTests {
    private static final int TIMEOUT = 300;

    private BeaconApiGameTests() {
    }

    /** An active beacon covers the positions in its area, for the full height of the world. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_api_area")
    public static void activeBeaconCoversItsArea(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        helper.assertTrue(BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST), "The beacon must accept");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "The beacon must be active"))
                .thenExecute(() -> {
                    helper.assertTrue(
                            BeaconholdApi.isCovered(level, beacon.offset(20, 0, 0), TestFunctions.TEST),
                            "The edge of the area (20 blocks) must be covered");
                    helper.assertTrue(
                            BeaconholdApi.isCovered(level, beacon.offset(0, 150, -20), TestFunctions.TEST),
                            "The area must go to the top of the world");
                    helper.assertTrue(
                            BeaconholdApi.isCovered(level, beacon.offset(0, -50, 0), TestFunctions.TEST),
                            "The area must go to the bottom of the world");
                    helper.assertFalse(
                            BeaconholdApi.isCovered(level, beacon.offset(21, 0, 0), TestFunctions.TEST),
                            "A position at 21 blocks must not be covered");
                    List<ActiveBeacon> covering =
                            BeaconholdApi.beaconsCovering(level, beacon.offset(5, 0, 5), TestFunctions.TEST);
                    helper.assertTrue(covering.size() == 1, "One beacon must cover the position");
                    ActiveBeacon active = covering.get(0);
                    helper.assertTrue(active.pos().equals(beacon), "The position of the beacon");
                    helper.assertTrue(active.pyramidLevel() == 1, "The pyramid level must be 1");
                    helper.assertTrue(active.area().radius() == 20, "The radius must be 20");
                    helper.assertTrue(
                            BeaconholdApi.activeBeacons(level).contains(active), "The list of active beacons");
                    helper.assertTrue(
                            BeaconholdApi.getFunction(level, beacon).equals(Optional.of(TestFunctions.TEST)),
                            "The function of the beacon");
                    BeaconholdApi.setFunction(level, beacon, null);
                    helper.assertFalse(
                            BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST),
                            "A beacon with no function covers nothing");
                    helper.assertTrue(BeaconholdApi.getFunction(level, beacon).isEmpty(), "No function");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon with no pyramid has no active function. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_api_no_pyramid")
    public static void beaconWithNoPyramidCoversNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 0);
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertFalse(
                            BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "No pyramid: no area");
                    helper.assertTrue(
                            BeaconholdApi.getFunction(level, beacon).isPresent(), "The beacon keeps its function");
                    helper.assertTrue(
                            TestFunctions.eventsAt(beacon).isEmpty(), "An inactive function posts no event");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon with a blocked beam stops its function. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_api_blocked")
    public static void blockedBeamStopsTheFunction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "The beacon must be active"))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE))
                .thenWaitUntil(() -> helper.assertFalse(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "A block stops the beam"))
                .thenExecute(() -> {
                    List<BeaconFunctionEvent> events = TestFunctions.eventsAt(beacon);
                    helper.assertTrue(events.size() == 2, "Two events: start, then stop");
                    helper.assertTrue(events.get(0) instanceof BeaconFunctionEvent.Start, "First a start event");
                    helper.assertTrue(events.get(1) instanceof BeaconFunctionEvent.Stop, "Then a stop event");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A larger pyramid stops the function with the old area, then starts it with the new area. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_api_level")
    public static void largerPyramidGivesALargerArea(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "The beacon must be active"))
                .thenExecute(() -> BeaconTestSupport.addLayer(helper, 2))
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon.offset(38, 0, 0), TestFunctions.TEST),
                        "Level 2: the radius must be 38"))
                .thenExecute(() -> {
                    List<BeaconFunctionEvent> events = TestFunctions.eventsAt(beacon);
                    helper.assertTrue(events.size() == 3, "Three events: start, stop, start");
                    helper.assertTrue(
                            events.get(1) instanceof BeaconFunctionEvent.Stop stop
                                    && stop.getBeacon().area().radius() == 20,
                            "The stop event has the old area");
                    helper.assertTrue(
                            events.get(2) instanceof BeaconFunctionEvent.Start start
                                    && start.getBeacon().pyramidLevel() == 2,
                            "The start event has the new pyramid level");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** The removal of a beacon stops its function and removes it from the saved list. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_api_removed")
    public static void removedBeaconStopsTheFunction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "The beacon must be active"))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON, Blocks.AIR))
                .thenWaitUntil(() -> helper.assertFalse(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "The beacon is removed"))
                .thenExecute(() -> {
                    List<BeaconFunctionEvent> events = TestFunctions.eventsAt(beacon);
                    helper.assertTrue(
                            events.get(events.size() - 1) instanceof BeaconFunctionEvent.Stop, "A stop event");
                    helper.assertTrue(BeaconholdApi.getFunction(level, beacon).isEmpty(), "No function");
                })
                .thenSucceed();
    }

    /** The API refuses a position with no beacon and a function that is not in the registry. */
    @GameTest(template = "beacon", batch = "beaconhold_api_errors")
    public static void setFunctionNeedsABeaconAndAKnownFunction(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos air = helper.absolutePos(BeaconTestSupport.BEACON);
        helper.assertFalse(BeaconholdApi.setFunction(level, air, TestFunctions.TEST), "No beacon: no function");
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        ResourceKey<BeaconFunction> unknown = ResourceKey.create(
                BeaconholdApi.FUNCTION_REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath("othermod", "unknown"));
        try {
            BeaconholdApi.setFunction(level, beacon, unknown);
            helper.fail("An unknown function must give an exception");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(BeaconholdApi.getFunction(level, beacon).isEmpty(), "The beacon keeps no function");
        }
        BeaconTestSupport.removeBeacon(helper);
        helper.succeed();
    }

    /**
     * A new radius in the server options gives an area change event with the old area and the new area. The function
     * stays active: no stop event and no start event. A change of the limit of the chunks gives no event.
     */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_api_area_change")
    public static void newRadiusGivesAnAreaChangeEvent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        int radius = config.radius(1);
        int newRadius = radius + 10;
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST), "The beacon must be active"))
                .thenExecute(() -> {
                    int limit = config.maxLoadedChunks();
                    try {
                        config.setMaxLoadedChunks(limit + 1);
                        ServerOptionChanges.apply(level.getServer());
                        helper.assertTrue(TestFunctions.eventsAt(beacon).size() == 1, "A new limit gives no event");
                        config.setRadius(1, newRadius);
                        ServerOptionChanges.apply(level.getServer());
                    } finally {
                        config.setMaxLoadedChunks(limit);
                        config.setRadius(1, radius);
                    }
                    List<BeaconFunctionEvent> events = TestFunctions.eventsAt(beacon);
                    helper.assertTrue(events.size() == 2, "Two events: start, then area change");
                    helper.assertTrue(
                            events.get(1) instanceof BeaconFunctionEvent.AreaChange change
                                    && change.getOldArea().radius() == radius
                                    && change.getBeacon().area().radius() == newRadius
                                    && change.getBeacon().function().equals(TestFunctions.TEST),
                            "The area change event has the old area and the new area");
                    ServerOptionChanges.apply(level.getServer());
                    List<BeaconFunctionEvent> after = TestFunctions.eventsAt(beacon);
                    helper.assertTrue(
                            after.size() == 3
                                    && after.get(2) instanceof BeaconFunctionEvent.AreaChange back
                                    && back.getOldArea().radius() == newRadius
                                    && back.getBeacon().area().radius() == radius,
                            "The old radius again gives a second area change event");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }
}
