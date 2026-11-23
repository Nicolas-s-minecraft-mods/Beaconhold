package io.github.nicolasgoyon.beaconhold.config;

import io.github.nicolasgoyon.beaconhold.function.LoadedChunks;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Applies a change of the server options to the active beacons. The areas, the loaded chunks and the domes use the new
 * values at once.
 */
public final class ServerOptionChanges {
    private ServerOptionChanges() {
    }

    /**
     * Adds the listener of the changes of the options file.
     *
     * @param modEventBus the event bus of the mod
     */
    public static void register(IEventBus modEventBus) {
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
     * the limit of the server, and the clients get the areas again.
     *
     * @param server the server
     */
    public static void apply(MinecraftServer server) {
        LoadedChunks.rebalance(server);
        for (ServerLevel level : server.getAllLevels()) {
            AreaSync.markChanged(level);
        }
    }
}
