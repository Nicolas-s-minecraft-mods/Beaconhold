package io.github.nicolasgoyon.beaconhold.client;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.client.dome.DomeAreas;
import io.github.nicolasgoyon.beaconhold.client.dome.DomeRenderer;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** The client part of the mod: the client options, the screen of the options in the mod list and the energy domes. */
@Mod(value = Beaconhold.MOD_ID, dist = Dist.CLIENT)
public final class BeaconholdClient {
    public BeaconholdClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BeaconholdConfig.CLIENT_SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, DomeRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, DomeRenderer::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> DomeAreas.clear());
    }
}
