package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.cost.LowStockWarnings;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of the warning for a low stock. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LowStockGameTests {
    private static final int TIMEOUT = 300;

    /** A fuel of less than one game day, with no stock. */
    private static final int LOW_FUEL = 2_000;

    private LowStockGameTests() {
    }

    /** Puts a mock player in the level, 10 blocks from the beacon. */
    private static Player addPlayer(GameTestHelper helper, BlockPos beacon) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = beacon.offset(10, 0, 0);
        ChunkPos chunk = new ChunkPos(pos);
        level.setChunkForced(chunk.x, chunk.z, true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        player.setNoGravity(true);
        level.addFreshEntity(player);
        return player;
    }

    private static void cleanUp(GameTestHelper helper, Player player) {
        player.discard();
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        helper.getLevel().setChunkForced(chunk.x, chunk.z, false);
        BeaconTestSupport.removeBeacon(helper);
    }

    /** An active beacon with less than one game day of fuel and stock warns the players in its area. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_low_stock")
    public static void lowStockWarnsThePlayersInTheArea(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        BeaconTestSupport.beaconEntity(helper).setData(ModRegistries.BEACON_FUEL, LOW_FUEL);
        Player player = addPlayer(helper, beacon);
        helper.startSequence()
                .thenWaitUntil(() -> {
                    LowStockWarnings.Warning warning = LowStockWarnings.last(level, beacon);
                    helper.assertTrue(warning != null, "The beacon must send a warning");
                    helper.assertTrue(warning.players() == 1, "One player is in the area");
                })
                .thenExecute(() -> cleanUp(helper, player))
                .thenSucceed();
    }

    /** A beacon with a full stack sends no warning. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_low_stock_full")
    public static void fullStockSendsNoWarning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeacon(helper, 1);
        Player player = addPlayer(helper, beacon);
        helper.startSequence()
                .thenExecuteAfter((int) BeaconTracker.UPDATE_INTERVAL * 3, () -> {
                    helper.assertTrue(
                            LowStockWarnings.last(level, beacon) == null, "A full stock must send no warning");
                    cleanUp(helper, player);
                })
                .thenSucceed();
    }
}
