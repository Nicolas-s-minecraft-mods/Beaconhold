package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.client.LowStockWarning;
import io.github.nicolasgoyon.beaconhold.client.dome.DomeAreas;
import io.github.nicolasgoyon.beaconhold.menu.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The payloads of the mod. */
public final class ModNetwork {
    /** The version of the protocol. A client and a server with different versions cannot connect. */
    private static final String VERSION = "1";

    private ModNetwork() {
    }

    /**
     * Registers the payloads and the synchronization of the areas.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, ModNetwork::registerPayloads);
        AreaSync.register();
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(
                AreaSyncPayload.TYPE, AreaSyncPayload.STREAM_CODEC, (payload, context) -> DomeAreas.set(payload.areas()));
        registrar.playToClient(
                LowStockPayload.TYPE, LowStockPayload.STREAM_CODEC, (payload, context) -> LowStockWarning.show(payload));
        registrar.playToServer(
                OpenFunctionsPayload.TYPE,
                OpenFunctionsPayload.STREAM_CODEC,
                (payload, context) -> ModMenus.openFunctions(context.player()));
    }
}
