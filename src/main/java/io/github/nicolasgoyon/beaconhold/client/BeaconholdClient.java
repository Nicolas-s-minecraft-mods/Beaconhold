package io.github.nicolasgoyon.beaconhold.client;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** The client part of the mod: the screen of the options in the mod list. */
@Mod(value = Beaconhold.MOD_ID, dist = Dist.CLIENT)
public final class BeaconholdClient {
    public BeaconholdClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
