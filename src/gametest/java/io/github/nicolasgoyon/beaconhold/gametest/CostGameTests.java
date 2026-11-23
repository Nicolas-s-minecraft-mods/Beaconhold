package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import io.github.nicolasgoyon.beaconhold.cost.CostRules;
import io.github.nicolasgoyon.beaconhold.menu.BeaconFunctionMenu;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** In-game tests of the cost of the functions: the stock of the consumable, the consumption and the drop. */
@GameTestHolder(Beaconhold.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CostGameTests {
    private static final int TIMEOUT = 400;

    /** The save test waits for the start, two updates and the tick before a periodic update. */
    private static final int SAVE_TIMEOUT = 700;

    /** The smallest number of ticks for each item, so that a test sees the consumption. */
    private static final int FAST_TICKS_PER_ITEM = CostRules.MIN_TICKS_PER_ITEM;

    private CostGameTests() {
    }

    private static boolean isActive(ServerLevel level, BlockPos beacon) {
        return BeaconholdApi.isCovered(level, beacon, TestFunctions.TEST);
    }

    private static int stockCount(GameTestHelper helper) {
        return BeaconStock.stock(BeaconTestSupport.beaconEntity(helper)).getCount();
    }

    /**
     * The stock, the fuel, the time of the last update of the fuel and the function stay after a save and a load of the
     * beacon, as after a restart of the server. The test puts the loaded beacon in the world one tick before a periodic
     * update. The beacon stays active and continues to use its fuel.
     */
    @GameTest(template = "beacon", timeoutTicks = SAVE_TIMEOUT, batch = "beaconhold_cost_save")
    public static void stockAndFuelStayAfterSaveAndLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        BeaconTestSupport.setStock(helper, new ItemStack(Items.QUARTZ, 10));
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        boolean[] watch = new boolean[1];
        boolean[] stopped = new boolean[1];
        int[] fuelAtSave = new int[1];
        helper.onEachTick(() -> {
            if (watch[0] && !isActive(level, beacon)) {
                stopped[0] = true;
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(isActive(level, beacon), "The function must start"))
                .thenIdle((int) BeaconTracker.UPDATE_INTERVAL + 1)
                .thenWaitUntil(() -> helper.assertTrue(
                        level.getGameTime() % BeaconTracker.UPDATE_INTERVAL == BeaconTracker.UPDATE_INTERVAL - 1,
                        "Wait for the tick before a periodic update"))
                .thenExecute(() -> {
                    BeaconBlockEntity saved = BeaconTestSupport.beaconEntity(helper);
                    int fuel = BeaconStock.fuel(saved);
                    long fuelTick = saved.getData(ModRegistries.BEACON_FUEL_TICK);
                    helper.assertTrue(fuel > 0, "The beacon must have fuel");
                    helper.assertTrue(
                            fuel < BeaconholdConfig.SERVER.ticksPerItem(), "The beacon must have used some fuel");
                    helper.assertTrue(fuelTick > 0L, "The beacon must keep the time of the last update");

                    CompoundTag tag = saved.saveWithFullMetadata(level.registryAccess());
                    BlockEntity loaded = BlockEntity.loadStatic(beacon, saved.getBlockState(), tag, level.registryAccess());
                    if (!(loaded instanceof BeaconBlockEntity loadedBeacon)) {
                        helper.fail("The tag must give a beacon");
                        return;
                    }
                    ItemStack stock = BeaconStock.stock(loadedBeacon);
                    helper.assertTrue(stock.is(Items.QUARTZ) && stock.getCount() == 9, "The stock must stay");
                    helper.assertTrue(BeaconStock.fuel(loadedBeacon) == fuel, "The fuel must stay");
                    helper.assertTrue(
                            loadedBeacon.getData(ModRegistries.BEACON_FUEL_TICK) == fuelTick,
                            "The time of the last update of the fuel must stay");
                    helper.assertTrue(
                            loadedBeacon.getExistingData(ModRegistries.BEACON_FUNCTION)
                                    .filter(TestFunctions.TEST.location()::equals)
                                    .isPresent(),
                            "The function must stay");
                    level.setBlockEntity(loadedBeacon);
                    fuelAtSave[0] = fuel;
                    watch[0] = true;
                })
                .thenIdle(2 * (int) BeaconTracker.UPDATE_INTERVAL + 2)
                .thenExecute(() -> {
                    watch[0] = false;
                    helper.assertFalse(stopped[0], "The loaded beacon must stay active");
                    helper.assertTrue(isActive(level, beacon), "The loaded beacon must be active");
                    helper.assertTrue(
                            BeaconStock.fuel(BeaconTestSupport.beaconEntity(helper)) < fuelAtSave[0],
                            "The loaded beacon must use its fuel");
                    helper.assertTrue(stockCount(helper) == 9, "The loaded beacon must not use a new item");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** Places a hopper next to the beacon that points to the beacon, with items in its first slot. */
    private static HopperBlockEntity hopperTo(GameTestHelper helper, Direction side, ItemStack items) {
        BlockPos relative = BeaconTestSupport.BEACON.relative(side);
        helper.setBlock(relative, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, side.getOpposite()));
        HopperBlockEntity hopper = helper.getBlockEntity(relative);
        hopper.setItem(0, items);
        return hopper;
    }

    /** A hopper puts consumable items into the stock. A hopper with other items puts nothing. Nothing comes out. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_cost_hopper")
    public static void hopperFillsTheStock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        HopperBlockEntity quartz = hopperTo(helper, Direction.EAST, new ItemStack(Items.QUARTZ, 5));
        HopperBlockEntity dirt = hopperTo(helper, Direction.WEST, new ItemStack(Items.DIRT, 5));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(stockCount(helper) == 5, "The hopper must fill the stock"))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(quartz.isEmpty(), "The quartz hopper must be empty");
                    helper.assertTrue(dirt.getItem(0).getCount() == 5, "The dirt must stay in its hopper");
                    helper.assertTrue(
                            BeaconStock.stock(BeaconTestSupport.beaconEntity(helper)).is(Items.QUARTZ),
                            "The stock must hold quartz only");
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, beacon, Direction.DOWN);
                    helper.assertTrue(handler != null, "The beacon must have an item capability");
                    helper.assertTrue(handler.extractItem(0, 64, false).isEmpty(), "Nothing comes out");
                    helper.assertTrue(stockCount(helper) == 5, "The stock must stay");
                    dirt.clearContent();
                    helper.setBlock(BeaconTestSupport.BEACON.east(), Blocks.AIR);
                    helper.setBlock(BeaconTestSupport.BEACON.west(), Blocks.AIR);
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon with no stock has no active function. A stock of a consumable starts the function. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_cost_no_stock")
    public static void functionNeedsAStock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        BeaconTestSupport.setStock(helper, new ItemStack(Items.DIRT, 64));
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenIdle(100)
                .thenExecute(() -> {
                    helper.assertFalse(isActive(level, beacon), "No consumable: the function is not active");
                    helper.assertTrue(stockCount(helper) == 64, "The beacon does not use an item out of the tag");
                    BeaconTestSupport.setStock(helper, new ItemStack(Items.QUARTZ, 10));
                })
                .thenWaitUntil(() -> helper.assertTrue(isActive(level, beacon), "A stock starts the function"))
                .thenExecute(() -> {
                    helper.assertTrue(stockCount(helper) == 9, "The start uses one item");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** Each item lasts the number of ticks of the option. With no stock, the function stops. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_cost_duration")
    public static void stockLastsItsTimeThenTheFunctionStops(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.SERVER.setTicksPerItem(FAST_TICKS_PER_ITEM);
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        BeaconTestSupport.setStock(helper, new ItemStack(Items.PRISMARINE_CRYSTALS, 2));
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        long[] start = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(isActive(level, beacon), "The function must start"))
                .thenExecute(() -> start[0] = helper.getTick())
                .thenWaitUntil(() -> helper.assertFalse(isActive(level, beacon), "The function must stop"))
                .thenExecute(() -> {
                    long duration = helper.getTick() - start[0];
                    BeaconholdConfig.SERVER.setTicksPerItem(CostRules.DEFAULT_TICKS_PER_ITEM);
                    helper.assertTrue(stockCount(helper) == 0, "The stock is empty");
                    helper.assertTrue(
                            duration >= 2L * FAST_TICKS_PER_ITEM && duration <= 3L * FAST_TICKS_PER_ITEM + 1,
                            "2 items last 2 periods, then an update stops the function. Ticks: " + duration);
                    helper.assertTrue(
                            BeaconholdApi.getFunction(level, beacon).isPresent(), "The beacon keeps its function");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** A beacon with a blocked beam does not use its stock. */
    @GameTest(template = "beacon", timeoutTicks = TIMEOUT, batch = "beaconhold_cost_blocked")
    public static void blockedBeamKeepsTheStock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BeaconholdConfig.SERVER.setTicksPerItem(FAST_TICKS_PER_ITEM);
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        BeaconTestSupport.setStock(helper, new ItemStack(Items.AMETHYST_SHARD, 20));
        helper.setBlock(BeaconTestSupport.BEACON.above(), Blocks.STONE);
        BeaconholdApi.setFunction(level, beacon, TestFunctions.TEST);
        helper.startSequence()
                .thenIdle(200)
                .thenExecute(() -> {
                    BeaconholdConfig.SERVER.setTicksPerItem(CostRules.DEFAULT_TICKS_PER_ITEM);
                    helper.assertFalse(isActive(level, beacon), "A blocked beam: no active function");
                    helper.assertTrue(stockCount(helper) == 20, "The beacon keeps its stock");
                    BeaconTestSupport.removeBeacon(helper);
                })
                .thenSucceed();
    }

    /** The removal of a beacon drops its stock. */
    @GameTest(template = "beacon", batch = "beaconhold_cost_drop")
    public static void removedBeaconDropsTheStock(GameTestHelper helper) {
        BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        BeaconTestSupport.setStock(helper, new ItemStack(Items.QUARTZ, 10));
        helper.setBlock(BeaconTestSupport.BEACON, Blocks.AIR);
        helper.assertItemEntityCountIs(Items.QUARTZ, BeaconTestSupport.BEACON, 2.0, 10);
        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.succeed();
    }

    /** The slot of the stock accepts only the consumables, and the shift click moves them into the slot. */
    @GameTest(template = "beacon", batch = "beaconhold_cost_slot")
    public static void stockSlotAcceptsOnlyTheConsumables(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos beacon = BeaconTestSupport.buildBeaconWithNoStock(helper, 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos near = helper.absolutePos(BeaconTestSupport.BEACON.east(2));
        player.setPos(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
        BeaconFunctionMenu menu = BeaconFunctionMenu.server(0, player.getInventory(), level, beacon, 3750);
        helper.assertTrue(
                menu.getSlot(BeaconFunctionMenu.STOCK_SLOT).mayPlace(new ItemStack(Items.QUARTZ)), "Quartz is valid");
        helper.assertFalse(
                menu.getSlot(BeaconFunctionMenu.STOCK_SLOT).mayPlace(new ItemStack(Items.DIRT)), "Dirt is not valid");

        // The slot 28 of the menu is the first slot of the hotbar.
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 5));
        helper.assertTrue(menu.quickMoveStack(player, 28).isEmpty(), "Dirt does not move");
        player.getInventory().setItem(0, new ItemStack(Items.AMETHYST_SHARD, 20));
        menu.quickMoveStack(player, 28);
        helper.assertTrue(stockCount(helper) == 20, "The shift click moves the shards into the stock");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "The hotbar slot is empty");
        helper.assertTrue(menu.remainingTicks() == 20L * 3750, "The menu gives the time of the stock");

        menu.quickMoveStack(player, BeaconFunctionMenu.STOCK_SLOT);
        helper.assertTrue(stockCount(helper) == 0, "The shift click takes the stock back");
        player.getInventory().clearContent();
        BeaconTestSupport.removeBeacon(helper);
        helper.succeed();
    }
}
