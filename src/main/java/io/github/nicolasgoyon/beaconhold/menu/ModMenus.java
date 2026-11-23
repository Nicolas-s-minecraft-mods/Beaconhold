package io.github.nicolasgoyon.beaconhold.menu;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.mixin.BeaconMenuAccessor;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The menus of the mod, and the rule that opens the function screen of a beacon. */
public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, BeaconholdApi.MOD_ID);

    /** The menu of the function screen of a beacon. */
    public static final Supplier<MenuType<BeaconFunctionMenu>> BEACON_FUNCTIONS =
            MENUS.register("beacon_functions", () -> IMenuTypeExtension.create(BeaconFunctionMenu::new));

    /** The title of the function screen. */
    public static final String TITLE = "container.beaconhold.beacon_functions";

    private ModMenus() {
    }

    /**
     * Registers the menus.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    /**
     * Gives the position of the beacon of the open beacon menu of a player.
     *
     * @param player the player
     * @return the position, or an empty value if the player has no open beacon menu
     */
    public static Optional<BlockPos> openBeacon(Player player) {
        if (player.containerMenu instanceof BeaconMenu menu) {
            return ((BeaconMenuAccessor) menu).getBeaconholdAccess().evaluate((level, pos) -> pos);
        }
        return Optional.empty();
    }

    /**
     * Opens the function screen for the beacon of the open beacon menu of a player. The player needs an open beacon
     * menu, so the rules of the beacon menu (distance, lock) also apply to the function screen.
     *
     * @param player the player
     * @return true if the server opens the function screen
     */
    public static boolean openFunctions(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Optional<BlockPos> beacon = openBeacon(player)
                .filter(pos -> level.getBlockEntity(pos) instanceof BeaconBlockEntity)
                .filter(pos -> player.containerMenu.stillValid(player));
        if (beacon.isEmpty()) {
            return false;
        }
        BlockPos pos = beacon.get();
        int ticksPerItem = BeaconholdConfig.SERVER.ticksPerItem();
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) ->
                        BeaconFunctionMenu.server(containerId, level, pos, ticksPerItem),
                Component.translatable(TITLE));
        return player.openMenu(provider, buffer -> buffer.writeVarInt(ticksPerItem)).isPresent();
    }
}
