package io.github.nicolasgoyon.beaconhold;

import com.mojang.logging.LogUtils;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.command.BeaconholdCommand;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.function.ModFunctions;
import io.github.nicolasgoyon.beaconhold.function.NoHostileSpawns;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/** The Beaconhold mod: more functions for the beacon, and an API so that other mods add functions to the beacon. */
@Mod(Beaconhold.MOD_ID)
public final class Beaconhold {
    /** The id of the mod. */
    public static final String MOD_ID = BeaconholdApi.MOD_ID;

    public static final Logger LOGGER = LogUtils.getLogger();

    public Beaconhold(IEventBus modEventBus, ModContainer container) {
        LOGGER.debug("{} {} starts", MOD_ID, container.getModInfo().getVersion());
        container.registerConfig(ModConfig.Type.SERVER, BeaconholdConfig.SERVER_SPEC);
        ModRegistries.register(modEventBus);
        ModFunctions.register(modEventBus);
        BeaconTracker.register();
        BeaconholdCommand.register();
        NoHostileSpawns.register();
        LoadedChunks.register(modEventBus);
    }
}
