package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconPower;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game tests of the vanilla powers of a beacon. The players in the area of the beacon get the powers. The default
 * radius for the pyramid level 2 is 38 blocks. The vanilla range is a square of 30 blocks around the beacon.
 */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class VanillaPowersGameTests {
    private static final int TIMEOUT = 300;

    /** The off test waits for the off state, one idle update, the start, and the update that gives the powers. */
    private static final int LONG_TIMEOUT = 600;

    /** A position out of the vanilla range and in the area. */
    private static final BlockPos IN_AREA_ONLY = new BlockPos(35, 0, 0);

    /** A position in the vanilla range and out of the area. */
    private static final BlockPos IN_VANILLA_RANGE_ONLY = new BlockPos(27, 0, 27);

    /** A position in the area and in the vanilla range, out of the template. */
    private static final BlockPos NEAR = new BlockPos(10, 0, 0);

    private VanillaPowersGameTests() {
    }

    /** Builds a beacon with the pyramid level 2, no function and the primary power Speed. */
    private static BlockPos speedBeacon(GameTestHelper helper, boolean withStock) {
        BlockPos beacon = withStock
                ? BeaconTestSupport.buildBeacon(helper, 2)
                : BeaconTestSupport.buildBeaconWithNoStock(helper, 2);
        BeaconBlockEntity entity = BeaconTestSupport.beaconEntity(helper);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = entity.saveWithoutMetadata(registries);
        tag.putString("primary_effect", "minecraft:speed");
        entity.loadWithComponents(tag, registries);
        return beacon;
    }

    /** Puts a mock player in the level, at a position relative to the beacon. */
    private static Player addPlayer(GameTestHelper helper, BlockPos beacon, BlockPos offset, List<ChunkPos> forced) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = beacon.offset(offset);
        ChunkPos chunk = new ChunkPos(pos);
        if (level.setChunkForced(chunk.x, chunk.z, true)) {
            forced.add(chunk);
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        player.setNoGravity(true);
        level.addFreshEntity(player);
        return player;
    }

    private static void cleanUp(GameTestHelper helper, List<Player> players, List<ChunkPos> forced) {
        for (Player player : players) {
            player.discard();
        }
        for (ChunkPos chunk : forced) {
            helper.getLevel().setChunkForced(chunk.x, chunk.z, false);
        }
        BeaconTestSupport.removeBeacon(helper);
    }

    /**
     * The players in the area get the powers, with the level bonus. A player out of the area gets no power. A beacon with
     * no function also uses its stock.
     */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_powers_area")
    public static void powersUseTheAreaOfTheBeacon(GameTestHelper helper) {
        BlockPos beacon = speedBeacon(helper, true);
        List<ChunkPos> forced = new ArrayList<>();
        Player inArea = addPlayer(helper, beacon, IN_AREA_ONLY, forced);
        Player inVanillaRange = addPlayer(helper, beacon, IN_VANILLA_RANGE_ONLY, forced);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        inArea.hasEffect(MobEffects.MOVEMENT_SPEED), "A player in the area must get Speed"))
                .thenExecute(() -> {
                    helper.assertFalse(
                            inVanillaRange.hasEffect(MobEffects.MOVEMENT_SPEED),
                            "A player out of the area must not get Speed");
                    MobEffectInstance speed = inArea.getEffect(MobEffects.MOVEMENT_SPEED);
                    int expected = BeaconholdConfig.SERVER.levelBonus();
                    helper.assertTrue(
                            speed != null && speed.getAmplifier() == expected,
                            "Speed must have the amplifier " + expected);
                    helper.assertTrue(
                            BeaconStock.stock(BeaconTestSupport.beaconEntity(helper)).getCount() < 64,
                            "A beacon with no function uses its stock");
                    cleanUp(helper, List.of(inArea, inVanillaRange), forced);
                })
                .thenSucceed();
    }

    /** With the option off, the powers use the vanilla range. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_powers_vanilla")
    public static void optionOffGivesTheVanillaRange(GameTestHelper helper) {
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        config.setVanillaPowersUseArea(false);
        BlockPos beacon = speedBeacon(helper, true);
        List<ChunkPos> forced = new ArrayList<>();
        Player inArea = addPlayer(helper, beacon, IN_AREA_ONLY, forced);
        Player inVanillaRange = addPlayer(helper, beacon, IN_VANILLA_RANGE_ONLY, forced);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        inVanillaRange.hasEffect(MobEffects.MOVEMENT_SPEED),
                        "A player in the vanilla range must get Speed"))
                .thenExecute(() -> {
                    config.setVanillaPowersUseArea(true);
                    helper.assertFalse(
                            inArea.hasEffect(MobEffects.MOVEMENT_SPEED),
                            "A player out of the vanilla range must not get Speed");
                    cleanUp(helper, List.of(inArea, inVanillaRange), forced);
                })
                .thenSucceed();
    }

    /** A beacon with no consumable is off: no power, and the clients hide its beam. A stock turns it on. */
    @GameTest(template = "beacon", timeoutTicks = LONG_TIMEOUT, batch = "beaconhold_powers_off")
    public static void beaconWithNoConsumableGivesNoPower(GameTestHelper helper) {
        BlockPos beacon = speedBeacon(helper, false);
        List<ChunkPos> forced = new ArrayList<>();
        Player near = addPlayer(helper, beacon, NEAR, forced);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconPower.isOffWithNoConsumable(BeaconTestSupport.beaconEntity(helper)),
                        "The beacon must be off with no consumable"))
                .thenIdle((int) BeaconTracker.UPDATE_INTERVAL + 1)
                .thenExecute(() -> {
                    helper.assertFalse(
                            near.hasEffect(MobEffects.MOVEMENT_SPEED), "A beacon that is off must give no power");
                    BeaconTestSupport.setStock(helper, new ItemStack(Items.AMETHYST_SHARD, 64));
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        near.hasEffect(MobEffects.MOVEMENT_SPEED), "A stock must turn the beacon on"))
                .thenExecute(() -> {
                    helper.assertFalse(
                            BeaconPower.isOff(BeaconTestSupport.beaconEntity(helper)), "The beacon must be on");
                    cleanUp(helper, List.of(near), forced);
                })
                .thenSucceed();
    }
}
