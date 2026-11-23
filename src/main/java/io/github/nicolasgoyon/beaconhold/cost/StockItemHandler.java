package io.github.nicolasgoyon.beaconhold.cost;

import java.util.function.Predicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The item capability of a beacon: one slot for the stock of the consumable. Hoppers and the pipes of other mods can
 * put consumable items into the stock, from all the sides. They cannot take items out.
 */
public final class StockItemHandler implements IItemHandler {
    private final BeaconBlockEntity beacon;
    private final Predicate<ItemStack> consumable;

    /**
     * Makes the item handler of a beacon. The handler accepts the items of the tag {@link ModTags#CONSUMABLES}.
     *
     * @param beacon the beacon
     */
    public StockItemHandler(BeaconBlockEntity beacon) {
        this(beacon, stack -> stack.is(ModTags.CONSUMABLES));
    }

    /**
     * Makes the item handler of a beacon with a rule for the consumable items. The unit tests use this constructor.
     *
     * @param beacon the beacon
     * @param consumable true for an item that the stock accepts
     */
    StockItemHandler(BeaconBlockEntity beacon, Predicate<ItemStack> consumable) {
        this.beacon = beacon;
        this.consumable = consumable;
    }

    /**
     * Registers the item capability of the beacons.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, BlockEntityType.BEACON, (beacon, side) -> new StockItemHandler(beacon)));
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? BeaconStock.stock(beacon) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        ItemStack stock = BeaconStock.stock(beacon);
        if (!stock.isEmpty() && !ItemStack.isSameItemSameComponents(stock, stack)) {
            return stack;
        }
        int limit = Math.min(getSlotLimit(slot), stack.getMaxStackSize());
        int accepted = Math.min(limit - stock.getCount(), stack.getCount());
        if (accepted <= 0) {
            return stack;
        }
        if (!simulate) {
            BeaconStock.setStock(beacon, stack.copyWithCount(stock.getCount() + accepted));
        }
        return stack.copyWithCount(stack.getCount() - accepted);
    }

    /** Gives no item: a hopper or a pipe cannot take the stock out of a beacon. */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return CostRules.STACK_SIZE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && consumable.test(stack);
    }
}
