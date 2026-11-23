package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-game tests of the function "no hostile spawns". Each test asks the game to finalize the spawn of a mob. Then it
 * checks if the function cancelled the spawn.
 */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NoHostileSpawnsGameTests {
    private static final int TIMEOUT = 300;

    private NoHostileSpawnsGameTests() {
    }

    /** Makes a mob at a position and finalizes its spawn. The mob is not in the level. */
    private static Mob finalizeSpawn(
            GameTestHelper helper, EntityType<? extends Mob> type, BlockPos pos, MobSpawnType spawnType) {
        ServerLevel level = helper.getLevel();
        Mob mob = type.create(level);
        helper.assertTrue(mob != null, "The game must make the mob");
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        EventHooks.finalizeMobSpawn(mob, level, level.getCurrentDifficultyAt(pos), spawnType, null);
        return mob;
    }

    private static BlockPos activeBeacon(GameTestHelper helper) {
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        BeaconholdApi.setFunction(helper.getLevel(), beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        return beacon;
    }

    private static void waitUntilActive(GameTestHelper helper, BlockPos beacon) {
        helper.assertTrue(
                BeaconholdApi.isCovered(helper.getLevel(), beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey()),
                "The beacon must be active");
    }

    /** A natural hostile spawn fails in the area and succeeds out of the area. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_spawn_natural")
    public static void naturalHostileSpawnFailsInTheArea(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper);
        helper.startSequence()
                .thenWaitUntil(() -> waitUntilActive(helper, beacon))
                .thenExecute(() -> {
                    Mob inside = finalizeSpawn(helper, EntityType.ZOMBIE, beacon.offset(15, 1, 0), MobSpawnType.NATURAL);
                    helper.assertTrue(inside.isSpawnCancelled(), "A zombie must not spawn in the area");
                    helper.assertFalse(
                            helper.getLevel().addFreshEntity(inside), "The level must refuse the cancelled zombie");
                    Mob edge = finalizeSpawn(helper, EntityType.SKELETON, beacon.offset(0, 30, 20), MobSpawnType.NATURAL);
                    helper.assertTrue(edge.isSpawnCancelled(), "A skeleton on the edge of the area must not spawn");
                    Mob outside =
                            finalizeSpawn(helper, EntityType.ZOMBIE, beacon.offset(21, 1, 0), MobSpawnType.NATURAL);
                    helper.assertFalse(outside.isSpawnCancelled(), "A zombie out of the area must spawn");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** The function does not stop the mobs that are not hostile. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_spawn_passive")
    public static void passiveMobSpawnsInTheArea(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper);
        helper.startSequence()
                .thenWaitUntil(() -> waitUntilActive(helper, beacon))
                .thenExecute(() -> {
                    Mob cow = finalizeSpawn(helper, EntityType.COW, beacon.offset(5, 1, 5), MobSpawnType.NATURAL);
                    helper.assertFalse(cow.isSpawnCancelled(), "A cow must spawn in the area");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** The function does not stop the patrols, the raids and the trial spawners. It stops the spawners only with the option. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_spawn_types")
    public static void otherSpawnTypesFollowTheRules(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper);
        BlockPos pos = beacon.offset(5, 1, 5);
        helper.startSequence()
                .thenWaitUntil(() -> waitUntilActive(helper, beacon))
                .thenExecute(() -> {
                    helper.assertFalse(
                            finalizeSpawn(helper, EntityType.PILLAGER, pos, MobSpawnType.PATROL).isSpawnCancelled(),
                            "A patrol must spawn");
                    helper.assertFalse(
                            finalizeSpawn(helper, EntityType.VINDICATOR, pos, MobSpawnType.EVENT).isSpawnCancelled(),
                            "A raid must spawn");
                    helper.assertFalse(
                            finalizeSpawn(helper, EntityType.ZOMBIE, pos, MobSpawnType.TRIAL_SPAWNER)
                                    .isSpawnCancelled(),
                            "A trial spawner must spawn");
                    BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
                    boolean before = config.blockSpawners();
                    try {
                        config.setBlockSpawners(false);
                        helper.assertFalse(
                                finalizeSpawn(helper, EntityType.ZOMBIE, pos, MobSpawnType.SPAWNER).isSpawnCancelled(),
                                "Option off: a spawner must spawn");
                        config.setBlockSpawners(true);
                        helper.assertTrue(
                                finalizeSpawn(helper, EntityType.ZOMBIE, pos, MobSpawnType.SPAWNER).isSpawnCancelled(),
                                "Option on: a spawner must not spawn");
                    } finally {
                        config.setBlockSpawners(before);
                    }
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon stops its protection when a block stops its beam. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_spawn_blocked")
    public static void blockedBeaconProtectsNothing(GameTestHelper helper) {
        BlockPos beacon = activeBeacon(helper);
        helper.startSequence()
                .thenWaitUntil(() -> waitUntilActive(helper, beacon))
                .thenExecute(() -> helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE))
                .thenWaitUntil(() -> helper.assertFalse(
                        BeaconholdApi.isCovered(helper.getLevel(), beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey()),
                        "A block stops the beam"))
                .thenExecute(() -> {
                    Mob zombie = finalizeSpawn(helper, EntityType.ZOMBIE, beacon.offset(5, 2, 0), MobSpawnType.NATURAL);
                    helper.assertFalse(zombie.isSpawnCancelled(), "A blocked beacon must not stop the spawn");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon with no pyramid protects nothing. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_spawn_no_pyramid")
    public static void beaconWithNoPyramidProtectsNothing(GameTestHelper helper) {
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 0);
        BeaconholdApi.setFunction(helper.getLevel(), beacon, ModFunctions.NO_HOSTILE_SPAWNS.getKey());
        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    Mob zombie = finalizeSpawn(helper, EntityType.ZOMBIE, beacon.offset(5, 1, 0), MobSpawnType.NATURAL);
                    helper.assertFalse(zombie.isSpawnCancelled(), "A beacon with no pyramid must not stop the spawn");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }
}
