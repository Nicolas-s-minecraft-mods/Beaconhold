package io.github.nicolasgoyon.beaconhold.function;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The beacon functions of Beaconhold. */
public final class ModFunctions {
    private static final DeferredRegister<BeaconFunction> FUNCTIONS =
            DeferredRegister.create(BeaconholdApi.FUNCTION_REGISTRY_KEY, BeaconholdApi.MOD_ID);

    /** Hostile mobs do not spawn in the area of the beacon. */
    public static final DeferredHolder<BeaconFunction, BeaconFunction> NO_HOSTILE_SPAWNS = FUNCTIONS.register(
            "no_hostile_spawns", () -> BeaconFunction.builder().icon(() -> Items.ROTTEN_FLESH).build());

    private ModFunctions() {
    }

    /**
     * Registers the functions.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }
}
