package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconOwner;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.config.ServerOptionChanges;
import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import io.github.nicolasgoyon.beaconhold.function.ChunkRules;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-game tests of the owner of a beacon and of the limit of the loaded chunks for each player. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChunkOwnerGameTests {
    private static final int TIMEOUT = 300;

    private static final UUID OWNER = UUID.fromString("6c1e0a52-8f3b-4d27-b9e4-2a7d5c3f9b18");

    private static final BlockPos FIRST = new BlockPos(2, 2, 2);

    private static final BlockPos SECOND = new BlockPos(10, 2, 2);

    private ChunkOwnerGameTests() {
    }

    /** A player that places a beacon becomes its owner. A beacon from a command has no owner. */
    @GameTest(template = "beacon", batch = "beaconhold_owner_place")
    public static void placedBeaconSavesItsOwner(GameTestHelper helper) {
        BlockPos below = BeaconTestSupport.BEACON.below();
        helper.setBlock(below, Blocks.IRON_BLOCK);
        BlockPos absoluteBelow = helper.absolutePos(below);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(Items.BEACON);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit =
                new BlockHitResult(Vec3.atCenterOf(absoluteBelow).add(0.0, 0.5, 0.0), Direction.UP, absoluteBelow, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertBlockPresent(Blocks.BEACON, BeaconTestSupport.BEACON);
        BeaconBlockEntity placed = BeaconTestSupport.beaconEntity(helper);
        helper.assertTrue(player.getUUID().equals(BeaconOwner.get(placed)), "The player must be the owner");
        BeaconTestSupport.removeBeacon(helper);

        helper.setBlock(BeaconTestSupport.BEACON, Blocks.BEACON);
        helper.assertTrue(
                BeaconOwner.get(BeaconTestSupport.beaconEntity(helper)) == null,
                "A beacon from a command must have no owner");
        BeaconTestSupport.removeBeacon(helper);
        helper.succeed();
    }

    /**
     * The limit for each player gives no chunk to a beacon whose owner has no free capacity. A beacon with no owner has
     * only the limit of the server. A change of the option gives the chunks again.
     */
    @GameTest(template = "two_beacons", timeoutTicks = TIMEOUT, batch = "beaconhold_owner_limit")
    public static void playerLimitAppliesOnlyToBeaconsWithAnOwner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        BlockPos noOwner = buildBeaconAt(helper, FIRST, null);
        BlockPos owned = buildBeaconAt(helper, SECOND, OWNER);
        int noOwnerArea = ChunkRules.areaChunks(noOwner.getX(), noOwner.getZ(), config.radius(1)).size();
        int ownedArea = ChunkRules.areaChunks(owned.getX(), owned.getZ(), config.radius(1)).size();
        config.setMaxChunksPerPlayer(0);
        BeaconholdApi.setFunction(level, noOwner, ModFunctions.LOADED_CHUNKS.getKey());
        BeaconholdApi.setFunction(level, owned, ModFunctions.LOADED_CHUNKS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, owned, ModFunctions.LOADED_CHUNKS.getKey())
                                && BeaconholdApi.isCovered(level, noOwner, ModFunctions.LOADED_CHUNKS.getKey()),
                        "The two beacons must be active"))
                .thenExecute(() -> {
                    int noOwnerChunks = LoadedChunks.chunksOf(level, noOwner).size();
                    int ownedChunks = LoadedChunks.chunksOf(level, owned).size();
                    config.setMaxChunksPerPlayer(ChunkRules.DEFAULT_MAX_CHUNKS_PER_PLAYER);
                    helper.assertTrue(
                            OWNER.equals(LoadedChunks.ownerOf(level, owned)), "The record must keep the owner");
                    helper.assertTrue(
                            noOwnerChunks == noOwnerArea,
                            "A beacon with no owner must load its area, got " + noOwnerChunks);
                    helper.assertTrue(
                            ownedChunks == 0, "The limit 0 must give no chunk to the owner, got " + ownedChunks);
                    ServerOptionChanges.apply(level.getServer());
                    int after = LoadedChunks.chunksOf(level, owned).size();
                    helper.assertTrue(
                            after == ownedArea, "A larger limit must give the area to the owner, got " + after);
                    helper.assertTrue(
                            LoadedChunks.ownerChunks(level, OWNER) == ownedArea, "The owner loads the area");
                    removeBeaconAt(helper, FIRST);
                    removeBeaconAt(helper, SECOND);
                })
                .thenSucceed();
    }

    /** The limit for each player reduces the chunks of the second beacon of the same owner. */
    @GameTest(template = "two_beacons", timeoutTicks = TIMEOUT, batch = "beaconhold_owner_second")
    public static void playerLimitReducesTheSecondBeacon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.Server config = BeaconholdConfig.SERVER;
        BlockPos first = buildBeaconAt(helper, FIRST, OWNER);
        BlockPos second = buildBeaconAt(helper, SECOND, OWNER);
        int firstArea = ChunkRules.areaChunks(first.getX(), first.getZ(), config.radius(1)).size();
        config.setMaxChunksPerPlayer(firstArea + 3);
        BeaconholdApi.setFunction(level, first, ModFunctions.LOADED_CHUNKS.getKey());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, first, ModFunctions.LOADED_CHUNKS.getKey()),
                        "The first beacon must be active"))
                .thenExecute(() -> BeaconholdApi.setFunction(level, second, ModFunctions.LOADED_CHUNKS.getKey()))
                .thenWaitUntil(() -> helper.assertTrue(
                        BeaconholdApi.isCovered(level, second, ModFunctions.LOADED_CHUNKS.getKey()),
                        "The second beacon must be active"))
                .thenExecute(() -> {
                    int firstChunks = LoadedChunks.chunksOf(level, first).size();
                    int secondChunks = LoadedChunks.chunksOf(level, second).size();
                    config.setMaxChunksPerPlayer(ChunkRules.DEFAULT_MAX_CHUNKS_PER_PLAYER);
                    removeBeaconAt(helper, FIRST);
                    removeBeaconAt(helper, SECOND);
                    helper.assertTrue(firstChunks == firstArea, "The first beacon loads its area, got " + firstChunks);
                    helper.assertTrue(
                            secondChunks == 3, "The limit gives 3 chunks to the second beacon, got " + secondChunks);
                })
                .thenSucceed();
    }

    /** Builds a beacon at the pyramid level 1 with a stock, at a relative position of the template "two_beacons". */
    private static BlockPos buildBeaconAt(GameTestHelper helper, BlockPos relative, @Nullable UUID owner) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                helper.setBlock(relative.offset(x, -1, z), Blocks.IRON_BLOCK);
            }
        }
        helper.setBlock(relative, Blocks.BEACON);
        BeaconBlockEntity entity = helper.getBlockEntity(relative);
        BeaconStock.setStock(entity, new ItemStack(Items.AMETHYST_SHARD, 64));
        if (owner != null) {
            BeaconOwner.set(entity, owner);
        }
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
}
