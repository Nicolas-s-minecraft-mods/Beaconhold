package io.github.nicolasgoyon.beaconhold.cost;

import io.github.nicolasgoyon.beaconhold.beacon.PowerRules;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/** The stock of the consumable and the fuel of a beacon. The beacon saves them as data attachments. */
public final class BeaconStock {
    private BeaconStock() {
    }

    /**
     * Gives the stock of a beacon. The menu changes this stack directly, then calls {@link #setChanged}.
     *
     * @param beacon the beacon
     * @return the stack of the stock. It is empty when the beacon has no stock.
     */
    public static ItemStack stock(BeaconBlockEntity beacon) {
        return beacon.getData(ModRegistries.BEACON_STOCK);
    }

    /**
     * Sets the stock of a beacon.
     *
     * @param beacon the beacon
     * @param stack the new stock
     */
    public static void setStock(BeaconBlockEntity beacon, ItemStack stack) {
        beacon.setData(ModRegistries.BEACON_STOCK, stack);
        setChanged(beacon);
    }

    /**
     * Gives the fuel of a beacon.
     *
     * @param beacon the beacon
     * @return the number of ticks that the last item still gives
     */
    public static int fuel(BeaconBlockEntity beacon) {
        return beacon.getData(ModRegistries.BEACON_FUEL);
    }

    /**
     * Marks the beacon as changed, so that the game saves it.
     *
     * @param beacon the beacon
     */
    public static void setChanged(BeaconBlockEntity beacon) {
        beacon.setChanged();
    }

    /**
     * Gives the number of items of the stock that the beacon can use. An item that is not in the tag
     * {@link ModTags#CONSUMABLES} gives no fuel.
     *
     * @param stack the stock
     * @return the number of usable items
     */
    public static int usableCount(ItemStack stack) {
        return stack.is(ModTags.CONSUMABLES) ? stack.getCount() : 0;
    }

    /**
     * Uses the stock of a beacon for one update, and gives a sign when the stock is low.
     *
     * @param level the dimension of the beacon
     * @param beacon the beacon
     * @param wasActive true if the beacon was on after the last update
     * @param canOperate true if the beacon has a pyramid and a free beam
     * @param periodic true for the update at the interval of the vanilla powers. Only this update gives the sign.
     * @return true if the beacon is on: it can operate, and it has fuel or the server option
     *     {@code cost.consumableRequired} is off
     */
    public static boolean operate(
            ServerLevel level, BeaconBlockEntity beacon, boolean wasActive, boolean canOperate, boolean periodic) {
        if (!BeaconholdConfig.SERVER.consumableRequired()) {
            // The vanilla rule: the beacon needs no consumable, and it keeps its stock and its fuel.
            beacon.setData(ModRegistries.BEACON_FUEL_TICK, level.getGameTime());
            return PowerRules.isOn(false, canOperate, false);
        }
        ItemStack stock = stock(beacon);
        int usable = usableCount(stock);
        int fuel = fuel(beacon);
        long now = level.getGameTime();
        int elapsed = FuelRules.elapsed(now, beacon.getData(ModRegistries.BEACON_FUEL_TICK), wasActive);
        int ticksPerItem = BeaconholdConfig.SERVER.ticksPerItem();
        FuelRules.State state = FuelRules.update(usable, fuel, ticksPerItem, elapsed, canOperate);
        if (state.stock() != usable) {
            setStock(beacon, state.stock() > 0 ? stock.copyWithCount(state.stock()) : ItemStack.EMPTY);
        }
        beacon.setData(ModRegistries.BEACON_FUEL, state.fuel());
        beacon.setData(ModRegistries.BEACON_FUEL_TICK, now);
        setChanged(beacon);
        long remaining = FuelRules.remainingTicks(state.stock(), state.fuel(), ticksPerItem);
        if (canOperate && periodic && FuelRules.isLow(remaining)) {
            BlockPos pos = beacon.getBlockPos();
            level.sendParticles(
                    ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 4, 0.2, 0.1, 0.2,
                    0.01);
        }
        return state.active();
    }

    /**
     * Drops the stock of a beacon at its position, and removes the stock and the fuel. The fuel of the last item is
     * lost.
     *
     * @param level the dimension of the beacon
     * @param beacon the beacon
     */
    public static void drop(Level level, BeaconBlockEntity beacon) {
        ItemStack stock = stock(beacon);
        BlockPos pos = beacon.getBlockPos();
        if (!stock.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stock.copy());
        }
        beacon.removeData(ModRegistries.BEACON_STOCK);
        beacon.removeData(ModRegistries.BEACON_FUEL);
        beacon.removeData(ModRegistries.BEACON_FUEL_TICK);
    }
}
