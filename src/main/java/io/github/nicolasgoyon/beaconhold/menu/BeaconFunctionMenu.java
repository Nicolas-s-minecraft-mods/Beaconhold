package io.github.nicolasgoyon.beaconhold.menu;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconRecord;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * The menu of the function screen of a beacon. The player selects one function, or no function. The menu shows the
 * function of the beacon, its state and the cost.
 *
 * <p>The buttons of the screen send their id to the server (see {@link FunctionButtons}). The server sets the function
 * of the beacon. The data slots give the function and the state to the client.
 */
public final class BeaconFunctionMenu extends AbstractContainerMenu {
    /** The data slot of the index of the function in the sorted list, or -1 for no function. */
    public static final int DATA_FUNCTION = 0;

    /** The data slot of the state of the function: 1 for active, 0 for not active. */
    public static final int DATA_ACTIVE = 1;

    /** The number of data slots. */
    public static final int DATA_COUNT = 2;

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
        this(containerId, ContainerLevelAccess.NULL, new SimpleContainerData(DATA_COUNT), buffer.readVarInt());
    }

    private BeaconFunctionMenu(int containerId, ContainerLevelAccess access, ContainerData data, int ticksPerItem) {
        super(ModMenus.BEACON_FUNCTIONS.get(), containerId);
        this.access = access;
        this.data = data;
        this.ticksPerItem = ticksPerItem;
        addDataSlots(data);
    }

    /**
     * Makes the menu on the server.
     *
     * @param containerId the id of the menu
     * @param level the dimension of the beacon
     * @param pos the position of the beacon
     * @param ticksPerItem the number of ticks for each consumable item
     * @return the menu
     */
    public static BeaconFunctionMenu server(int containerId, ServerLevel level, BlockPos pos, int ticksPerItem) {
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
        return new BeaconFunctionMenu(containerId, ContainerLevelAccess.create(level, pos), data, ticksPerItem);
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
     * Gives the number of ticks for each consumable item.
     *
     * @return the value of the server option {@code cost.ticksPerItem}
     */
    public int ticksPerItem() {
        return ticksPerItem;
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
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.BEACON);
    }
}
