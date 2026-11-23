package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
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

    /** A position out of the vanilla range and in the area. */
    private static final BlockPos IN_AREA_ONLY = new BlockPos(35, 0, 0);

    /** A position in the vanilla range and out of the area. */
    private static final BlockPos IN_VANILLA_RANGE_ONLY = new BlockPos(27, 0, 27);

    private VanillaPowersGameTests() {
    }

    /** Builds a beacon with the pyramid level 2, no function, no stock and the primary power Speed. */
    private static BlockPos speedBeacon(GameTestHelper helper) {
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 2);
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

    /** The players in the area get the powers. A player out of the area gets no power. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_powers_area")
    public static void powersUseTheAreaOfTheBeacon(GameTestHelper helper) {
        BlockPos beacon = speedBeacon(helper);
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
                    cleanUp(helper, List.of(inArea, inVanillaRange), forced);
                })
                .thenSucceed();
    }

    /** With the option off, the powers use the vanilla range. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_powers_vanilla")
    public static void optionOffGivesTheVanillaRange(GameTestHelper helper) {
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        config.setVanillaPowersUseArea(false);
        BlockPos beacon = speedBeacon(helper);
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
}
