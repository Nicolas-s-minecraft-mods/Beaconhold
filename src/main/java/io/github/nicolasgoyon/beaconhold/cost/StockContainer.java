package io.github.nicolasgoyon.beaconhold.cost;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/** A container of one slot for the stock of a beacon. The menu of the function screen uses it on the server. */
public final class StockContainer implements Container {
    private final BeaconBlockEntity beacon;

    /**
     * Makes a container for the stock of a beacon.
     *
     * @param beacon the beacon
     */
    public StockContainer(BeaconBlockEntity beacon) {
        this.beacon = beacon;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return BeaconStock.stock(beacon).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? BeaconStock.stock(beacon) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stock = BeaconStock.stock(beacon).copy();
        ItemStack removed = stock.split(amount);
        BeaconStock.setStock(beacon, stock);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack stock = BeaconStock.stock(beacon);
        BeaconStock.setStock(beacon, ItemStack.EMPTY);
        return stock;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            BeaconStock.setStock(beacon, stack);
        }
    }

    @Override
    public void setChanged() {
        BeaconStock.setChanged(beacon);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(beacon, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.is(ModTags.CONSUMABLES);
    }

    @Override
    public void clearContent() {
        BeaconStock.setStock(beacon, ItemStack.EMPTY);
    }
}
