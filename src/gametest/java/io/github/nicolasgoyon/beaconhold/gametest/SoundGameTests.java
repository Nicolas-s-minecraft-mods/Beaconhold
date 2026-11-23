package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconPower;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.CostRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of the sounds of a beacon. A beacon that is off is silent. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SoundGameTests {
    private static final int TIMEOUT = 600;

    private SoundGameTests() {
    }

    /** A beacon with no consumable plays no ambient sound, no activation sound and no sound when a player breaks it. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_sound_off")
    public static void beaconThatIsOffIsSilent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = helper.absolutePos(BeaconTestSupport.BEACON);
        BeaconSounds.clear(level, beacon);
        BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconPower.isOffWithNoConsumable(BeaconTestSupport.beaconEntity(helper)),
                        "The beacon must be off with no consumable"))
                .thenIdle((int) BeaconTracker.UPDATE_INTERVAL * 2)
                .thenExecute(() -> {
                    BeaconTestSupport.removeBeacon(helper);
                    helper.assertTrue(
                            BeaconSounds.countAll(level, beacon) == 0,
                            "A beacon that is off must be silent. Sounds: " + BeaconSounds.countAll(level, beacon));
                })
                .thenSucceed();
    }

    /**
     * A stock turns the beacon on: it plays the activation sound once, then the ambient sound. When the consumable ends,
     * the beacon plays the deactivation sound once, then it is silent.
     */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT * 2, batch = "beaconhold_sound_power")
    public static void stockGivesTheSoundsOfThePowerState(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = helper.absolutePos(BeaconTestSupport.BEACON);
        BeaconholdConfig.SERVER.setTicksPerItem(CostRules.MIN_TICKS_PER_ITEM);
        BeaconSounds.clear(level, beacon);
        BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconPower.isOffWithNoConsumable(BeaconTestSupport.beaconEntity(helper)),
                        "The beacon must be off with no consumable"))
                .thenExecute(() -> BeaconTestSupport.setStock(helper, new ItemStack(Items.AMETHYST_SHARD, 2)))
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconSounds.count(level, beacon, SoundEvents.BEACON_AMBIENT) > 0,
                        "A beacon that is on must play the ambient sound"))
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconPower.isOffWithNoConsumable(BeaconTestSupport.beaconEntity(helper)),
                        "The beacon must go off when the consumable ends"))
                .thenExecute(() -> {
                    helper.assertTrue(
                            BeaconSounds.count(level, beacon, SoundEvents.BEACON_ACTIVATE) == 1,
                            "The beacon must play the activation sound once");
                    helper.assertTrue(
                            BeaconSounds.count(level, beacon, SoundEvents.BEACON_DEACTIVATE) == 1,
                            "The beacon must play the deactivation sound once");
                    BeaconSounds.clear(level, beacon);
                })
                .thenIdle((int) BeaconTracker.UPDATE_INTERVAL * 2)
                .thenExecute(() -> {
                    BeaconholdConfig.SERVER.setTicksPerItem(CostRules.DEFAULT_TICKS_PER_ITEM);
                    BeaconTestSupport.removeBeacon(helper);
                    helper.assertTrue(
                            BeaconSounds.countAll(level, beacon) == 0, "A beacon that is off again must be silent");
                })
                .thenSucceed();
    }
}
