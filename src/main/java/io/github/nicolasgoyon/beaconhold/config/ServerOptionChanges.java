package io.github.nicolasgoyon.beaconhold.config;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconArea;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Applies a change of the server options to the active beacons. The areas, the loaded chunks and the domes use the new
 * values at once. Each active beacon whose radius changes gives a {@link BeaconFunctionEvent.AreaChange} event.
 */
public final class ServerOptionChanges {
    /** The radii that the server applied last, or null when no server runs. The server thread uses this value. */
    @Nullable
    private static int[] appliedRadii;

    private ServerOptionChanges() {
    }

    /**
     * Adds the listener of the changes of the options file.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(
                ServerStartedEvent.class, event -> appliedRadii = BeaconholdConfig.SERVER.radii());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> appliedRadii = null);
        modEventBus.addListener(ModConfigEvent.Reloading.class, event -> {
            if (event.getConfig().getSpec() != BeaconholdConfig.SERVER_SPEC) {
                return;
            }
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                // The event can come from the thread that watches the file.
                server.execute(() -> apply(server));
            }
        });
    }

    /**
     * Applies the current server options to the active beacons: the beacons load the chunks of their areas again, with
     * the limit of the server, and the clients get the areas again. Then each active beacon whose radius changes gives
     * a {@link BeaconFunctionEvent.AreaChange} event.
     *
     * @param server the server
     */
    public static void apply(MinecraftServer server) {
        int[] before = appliedRadii;
        int[] after = BeaconholdConfig.SERVER.radii();
        appliedRadii = after;
        LoadedChunks.rebalance(server);
        for (ServerLevel level : server.getAllLevels()) {
            AreaSync.markChanged(level);
        }
        if (before == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ActiveBeacon beacon : BeaconTracker.activeBeacons(level)) {
                int pyramidLevel = beacon.pyramidLevel();
                if (AreaRadii.radiusChanged(pyramidLevel, before, after)) {
                    BeaconArea oldArea = new BeaconArea(beacon.pos(), AreaRadii.radius(pyramidLevel, before));
                    NeoForge.EVENT_BUS.post(new BeaconFunctionEvent.AreaChange(level, beacon, oldArea));
                }
            }
        }
    }
}
