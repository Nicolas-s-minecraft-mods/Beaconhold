package io.github.nicolasgoyon.beaconhold;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** The Beaconhold mod: more functions for the beacon, and an API so that other mods add functions to the beacon. */
@Mod(Beaconhold.MOD_ID)
public final class Beaconhold {
    /** The id of the mod. */
    public static final String MOD_ID = "beaconhold";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Beaconhold(IEventBus modEventBus, ModContainer container) {
        LOGGER.debug("{} {} starts", MOD_ID, container.getModInfo().getVersion());
    }
}
