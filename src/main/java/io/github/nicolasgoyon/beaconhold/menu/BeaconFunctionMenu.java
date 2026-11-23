package io.github.nicolasgoyon.beaconhold.menu;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconPower;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconRecord;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import io.github.nicolasgoyon.beaconhold.cost.CostRules;
import io.github.nicolasgoyon.beaconhold.cost.FuelRules;
import io.github.nicolasgoyon.beaconhold.cost.ModTags;
import io.github.nicolasgoyon.beaconhold.cost.StockContainer;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * The menu of the function screen of a beacon. The player selects one function, or no function. The menu shows the
 * function of the beacon, its state and the cost. The player puts the stock of the consumable in the slot of the menu.
 *
 * <p>The buttons of the screen send their id to the server (see {@link FunctionButtons}). The server sets the function
 * of the beacon. The data slots give the function, the state, the fuel, the loaded chunks and the use of the owner to the client.
 */
public final class BeaconFunctionMenu extends AbstractContainerMenu {
    /** The data slot of the index of the function in the sorted list, or -1 for no function. */
    public static final int DATA_FUNCTION = 0;

    /** The data slot of the state of the function: 1 for active, 0 for not active. */
    public static final int DATA_ACTIVE = 1;

    /** The data slot of the fuel: the number of ticks that the last consumable item still gives. */
    public static final int DATA_FUEL = 2;

    /** The data slot of the number of chunks that the beacon loads. */
    public static final int DATA_CHUNKS = 3;

    /** The data slot of the number of chunks that the area of the active "loaded chunks" beacon touches. */
    public static final int DATA_AREA_CHUNKS = 4;

    /** The data slots of the number of chunks that all the beacons of the server load (see {@link DataValues}). */
    public static final int DATA_SERVER_CHUNKS_LOW = 5;

    public static final int DATA_SERVER_CHUNKS_HIGH = 6;

    /** The data slots of the limit of the server (see {@link DataValues}). */
    public static final int DATA_LIMIT_LOW = 7;

    public static final int DATA_LIMIT_HIGH = 8;

    /** The data slot of the power state: 1 when the beacon is off because it has no consumable, else 0. */
    public static final int DATA_OFF = 9;

    /** The data slots of the number of chunks that the beacons of the owner load (see {@link DataValues}). */
    public static final int DATA_OWNER_CHUNKS_LOW = 10;

    public static final int DATA_OWNER_CHUNKS_HIGH = 11;

    /** The data slots of the limit for each player (see {@link DataValues}). */
    public static final int DATA_PLAYER_LIMIT_LOW = 12;

    public static final int DATA_PLAYER_LIMIT_HIGH = 13;

    /** The data slot of the owner: 1 when the beacon has a known owner, else 0. */
    public static final int DATA_HAS_OWNER = 14;

    /** The number of data slots. */
    public static final int DATA_COUNT = 15;

    /** The index of the slot of the stock. */
    public static final int STOCK_SLOT = 0;

    /** The position of the slot of the stock in the screen. */
    public static final int STOCK_X = 8;

    public static final int STOCK_Y = 98;

    /** The position of the inventory of the player in the screen. */
    public static final int INVENTORY_X = 8;

    public static final int INVENTORY_Y = 168;

    /** The number of slots of the inventory of the player. */
    private static final int INVENTORY_SLOTS = 36;

    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int ticksPerItem;

    /**
     * Makes the menu on the client.
     *
     * @param containerId the id of the menu
     * @param inventory the inventory of the player
     * @param buffer the data from the server: the number of ticks for each consumable item
     */
    public BeaconFunctionMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                inventory,
                new SimpleContainer(1),
                ContainerLevelAccess.NULL,
                new SimpleContainerData(DATA_COUNT),
                buffer.readVarInt());
    }

    private BeaconFunctionMenu(
            int containerId,
            Inventory inventory,
            Container stock,
            ContainerLevelAccess access,
            ContainerData data,
            int ticksPerItem) {
        super(ModMenus.BEACON_FUNCTIONS.get(), containerId);
        this.access = access;
        this.data = data;
        this.ticksPerItem = ticksPerItem;
        addSlot(new StockSlot(stock, STOCK_X, STOCK_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        INVENTORY_X + column * 18,
                        INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    /**
     * Makes the menu on the server.
     *
     * @param containerId the id of the menu
     * @param inventory the inventory of the player
     * @param level the dimension of the beacon
     * @param pos the position of the beacon
     * @param ticksPerItem the number of ticks for each consumable item
     * @return the menu
     */
    public static BeaconFunctionMenu server(
            int containerId, Inventory inventory, ServerLevel level, BlockPos pos, int ticksPerItem) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_FUNCTION -> FunctionList.indexOf(BeaconTracker.getFunction(level, pos));
                    case DATA_ACTIVE -> BeaconTracker.getRecord(level, pos)
                                    .map(BeaconRecord::active)
                                    .orElse(false)
                            ? 1
                            : 0;
                    case DATA_FUEL -> level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon
                            ? BeaconStock.fuel(beacon)
                            : 0;
                    case DATA_CHUNKS -> LoadedChunks.chunksOf(level, pos).size();
                    case DATA_AREA_CHUNKS -> LoadedChunks.areaChunkCount(level, pos);
                    case DATA_SERVER_CHUNKS_LOW -> DataValues.low(LoadedChunks.totalLoadedChunks(level));
                    case DATA_SERVER_CHUNKS_HIGH -> DataValues.high(LoadedChunks.totalLoadedChunks(level));
                    case DATA_LIMIT_LOW -> DataValues.low(BeaconholdConfig.SERVER.maxLoadedChunks());
                    case DATA_LIMIT_HIGH -> DataValues.high(BeaconholdConfig.SERVER.maxLoadedChunks());
                    case DATA_OFF -> level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon
                                    && BeaconPower.isOffWithNoConsumable(beacon)
                            ? 1
                            : 0;
                    case DATA_OWNER_CHUNKS_LOW -> DataValues.low(ownerChunks(level, pos));
                    case DATA_OWNER_CHUNKS_HIGH -> DataValues.high(ownerChunks(level, pos));
                    case DATA_PLAYER_LIMIT_LOW -> DataValues.low(BeaconholdConfig.SERVER.maxChunksPerPlayer());
                    case DATA_PLAYER_LIMIT_HIGH -> DataValues.high(BeaconholdConfig.SERVER.maxChunksPerPlayer());
                    case DATA_HAS_OWNER -> LoadedChunks.ownerOf(level, pos) != null ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // The server reads the values from the beacon. The client cannot change them.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        Container stock = level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon
                ? new StockContainer(beacon)
                : new SimpleContainer(1);
        return new BeaconFunctionMenu(
                containerId, inventory, stock, ContainerLevelAccess.create(level, pos), data, ticksPerItem);
    }

    /**
     * Gives the index of the function of the beacon.
     *
     * @return the index in the sorted list, or -1 for no function
     */
    public int functionIndex() {
        return data.get(DATA_FUNCTION);
    }

    /**
     * Tells if the function of the beacon is active.
     *
     * @return true if the function is active
     */
    public boolean isActive() {
        return data.get(DATA_ACTIVE) != 0;
    }

    /**
     * Tells if the beacon is off because it has no consumable.
     *
     * @return true if the beacon has a pyramid and a free beam, but no fuel and no stock
     */
    public boolean isOff() {
        return data.get(DATA_OFF) != 0;
    }

    /**
     * Gives the fuel of the beacon.
     *
     * @return the number of ticks that the last consumable item still gives
     */
    public int fuel() {
        return data.get(DATA_FUEL);
    }

    /**
     * Tells if the function of the beacon is "loaded chunks".
     *
     * @return true if the beacon has the function "loaded chunks"
     */
    public boolean hasLoadedChunks() {
        int index = functionIndex();
        List<ResourceKey<BeaconFunction>> functions = FunctionList.sorted();
        return index >= 0
                && index < functions.size()
                && functions.get(index).equals(ModFunctions.LOADED_CHUNKS.getKey());
    }

    /**
     * Gives the number of chunks that the beacon loads.
     *
     * @return the number of chunks
     */
    public int loadedChunks() {
        return data.get(DATA_CHUNKS);
    }

    /**
     * Gives the number of chunks that the area of the beacon touches, when the function "loaded chunks" is active.
     *
     * @return the number of chunks, or 0 when the function "loaded chunks" is not active
     */
    public int areaChunks() {
        return data.get(DATA_AREA_CHUNKS);
    }

    /**
     * Gives the number of chunks that all the beacons of the server load.
     *
     * @return the number of chunks
     */
    public int serverChunks() {
        return DataValues.join(data.get(DATA_SERVER_CHUNKS_LOW), data.get(DATA_SERVER_CHUNKS_HIGH));
    }

    /**
     * Gives the limit of the server.
     *
     * @return the value of the server option {@code loadedChunks.maxLoadedChunks}
     */
    public int serverLimit() {
        return DataValues.join(data.get(DATA_LIMIT_LOW), data.get(DATA_LIMIT_HIGH));
    }

    /**
     * Tells if the beacon has a known owner: the player that placed it.
     *
     * @return true if the beacon has a known owner
     */
    public boolean hasOwner() {
        return data.get(DATA_HAS_OWNER) == 1;
    }

    /**
     * Gives the number of chunks that all the beacons of the owner of the beacon load.
     *
     * @return the number of chunks, or 0 when the beacon has no known owner
     */
    public int ownerChunks() {
        return DataValues.join(data.get(DATA_OWNER_CHUNKS_LOW), data.get(DATA_OWNER_CHUNKS_HIGH));
    }

    /**
     * Gives the limit for each player.
     *
     * @return the value of the server option {@code loadedChunks.maxChunksPerPlayer}
     */
    public int playerLimit() {
        return DataValues.join(data.get(DATA_PLAYER_LIMIT_LOW), data.get(DATA_PLAYER_LIMIT_HIGH));
    }

    /**
     * Gives the time of operation in the stock and in the fuel of the beacon.
     *
     * @return the time in ticks
     */
    public long remainingTicks() {
        return FuelRules.remainingTicks(BeaconStock.usableCount(getSlot(STOCK_SLOT).getItem()), fuel(), ticksPerItem);
    }

    /**
     * Gives the number of ticks for each consumable item.
     *
     * @return the value of the server option {@code cost.ticksPerItem}
     */
    public int ticksPerItem() {
        return ticksPerItem;
    }

    private static int ownerChunks(ServerLevel level, BlockPos pos) {
        UUID owner = LoadedChunks.ownerOf(level, pos);
        return owner == null ? 0 : LoadedChunks.ownerChunks(level, owner);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == FunctionButtons.BACK) {
            access.execute((level, pos) -> {
                if (level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon) {
                    player.openMenu(beacon);
                }
            });
            return true;
        }
        List<ResourceKey<BeaconFunction>> functions = FunctionList.sorted();
        if (!FunctionButtons.isSelection(id, functions.size())) {
            return false;
        }
        @Nullable ResourceKey<BeaconFunction> function =
                id == FunctionButtons.NONE ? null : functions.get(FunctionButtons.functionOf(id));
        access.execute((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                BeaconTracker.setFunction(serverLevel, pos, function);
            }
        });
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index == STOCK_SLOT) {
            if (!moveItemStackTo(stack, 1, 1 + INVENTORY_SLOTS, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.is(ModTags.CONSUMABLES) || !moveItemStackTo(stack, STOCK_SLOT, STOCK_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.BEACON);
    }

    /** The slot of the stock: it accepts one stack of the items of the tag {@link ModTags#CONSUMABLES}. */
    private static final class StockSlot extends Slot {
        StockSlot(Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModTags.CONSUMABLES);
        }

        @Override
        public int getMaxStackSize() {
            return CostRules.STACK_SIZE;
        }
    }
}
