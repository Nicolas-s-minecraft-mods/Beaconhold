package io.github.nicolasgoyon.beaconhold.client;

import io.github.nicolasgoyon.beaconhold.Beaconhold;
import io.github.nicolasgoyon.beaconhold.client.dome.DomeAreas;
import io.github.nicolasgoyon.beaconhold.client.dome.DomeRenderer;
import io.github.nicolasgoyon.beaconhold.client.screen.BeaconFunctionScreen;
import io.github.nicolasgoyon.beaconhold.client.screen.BeaconScreenButton;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.menu.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client part of the mod: the client options, the screen of the options in the mod list, the energy domes and the
 * function screen of the beacon.
 */
@Mod(value = Beaconhold.MOD_ID, dist = Dist.CLIENT)
public final class BeaconholdClient {
    public BeaconholdClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, BeaconholdConfig.CLIENT_SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, DomeRenderer::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, DomeRenderer::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> DomeAreas.clear());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> LowStockWarning.clear());
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, BeaconScreenButton::onScreenInit);
        modEventBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(
                ModMenus.BEACON_FUNCTIONS.get(), BeaconFunctionScreen::new));
    }
}
