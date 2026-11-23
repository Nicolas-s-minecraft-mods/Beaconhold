package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends the areas of the active beacons to the players, so that the clients show the energy domes.
 *
 * <p>A change of an active function marks its dimension. At the end of the tick, the server sends the areas of each
 * marked dimension to the players in this dimension. A player gets the areas of a dimension when he enters it.
 */
public final class AreaSync {
    private static final Set<ServerLevel> CHANGED = Collections.newSetFromMap(new WeakHashMap<>());

    private AreaSync() {
    }

    /** Adds the listeners of the synchronization to the NeoForge event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, AreaSync::onLevelTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> sendTo(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(
                PlayerEvent.PlayerChangedDimensionEvent.class, event -> sendTo(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> sendTo(event.getEntity()));
    }

    /**
     * Marks a dimension: its areas changed. The server sends the areas at the end of the tick.
     *
     * @param level the dimension
     */
    public static synchronized void markChanged(ServerLevel level) {
        CHANGED.add(level);
    }

    /**
     * Tells if the areas of a dimension changed since the last send.
     *
     * @param level the dimension
     * @return true if the server sends the areas at the end of the tick
     */
    public static synchronized boolean isChanged(ServerLevel level) {
        return CHANGED.contains(level);
    }

    private static synchronized boolean takeChanged(ServerLevel level) {
        return CHANGED.remove(level);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && takeChanged(level)) {
            PacketDistributor.sendToPlayersInDimension(level, new AreaSyncPayload(areas(level)));
        }
    }

    private static void sendTo(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new AreaSyncPayload(areas(serverPlayer.serverLevel())));
        }
    }

    /**
     * Gives the areas that the clients show in a dimension: the active beacons with a function that shows its area.
     *
     * @param level the dimension
     * @return the areas, at most {@link AreaSyncPayload#MAX_AREAS}
     */
    public static List<AreaSyncPayload.Area> areas(ServerLevel level) {
        List<AreaSyncPayload.Area> result = new ArrayList<>();
        for (ActiveBeacon beacon : BeaconTracker.activeBeacons(level)) {
            BeaconFunction function = ModRegistries.FUNCTIONS.get(beacon.function());
            if (function != null && function.showsArea() && result.size() < AreaSyncPayload.MAX_AREAS) {
                result.add(new AreaSyncPayload.Area(beacon.pos(), beacon.area().radius()));
            }
        }
        return result;
    }
}
