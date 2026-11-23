package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.beacon.BeamBeacons;
import io.github.nicolasgoyon.beaconhold.beacon.DomeRules;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
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
 * <p>The server sends to each player only the areas whose edge is near the player (see {@link AreaVisibility}). Each
 * {@link AreaVisibility#CHECK_INTERVAL} ticks, the server calculates the areas near each player of the dimension. It
 * also calculates them at the end of a tick with a change of an area. It sends the list only when the list changes. A
 * player also gets the areas when the player joins, enters a dimension or respawns.
 */
public final class AreaSync {
    private static final Set<ServerLevel> CHANGED = Collections.newSetFromMap(new WeakHashMap<>());

    /** The last list that the server sent to each player. The server thread uses this map. */
    private static final Map<ServerPlayer, List<AreaSyncPayload.Area>> SENT = new WeakHashMap<>();

    private AreaSync() {
    }

    /** Adds the listeners of the synchronization to the NeoForge event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, AreaSync::onLevelTick);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> sendTo(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(
                PlayerEvent.PlayerChangedDimensionEvent.class, event -> sendTo(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> sendTo(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> SENT.remove(event.getEntity()));
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
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean changed = takeChanged(level);
        if (!changed && level.getGameTime() % AreaVisibility.CHECK_INTERVAL != 0L) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        List<AreaSyncPayload.Area> all = areas(level);
        for (ServerPlayer player : players) {
            List<AreaSyncPayload.Area> near = near(all, player.getX(), player.getZ());
            if (!near.equals(SENT.get(player))) {
                send(player, near);
            }
        }
    }

    private static void sendTo(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            send(serverPlayer, areasNear(serverPlayer.serverLevel(), serverPlayer.getX(), serverPlayer.getZ()));
        }
    }

    private static void send(ServerPlayer player, List<AreaSyncPayload.Area> areas) {
        SENT.put(player, areas);
        PacketDistributor.sendToPlayer(player, new AreaSyncPayload(areas));
    }

    /**
     * Gives the areas that the clients show in a dimension. The list contains the active beacons with a function that
     * shows its area, then the loaded beacons with an active beam that show the dome (see {@link DomeRules}). Each
     * beacon is in the list one time.
     *
     * @param level the dimension
     * @return all the areas of the dimension
     */
    public static List<AreaSyncPayload.Area> areas(ServerLevel level) {
        Map<BlockPos, AreaSyncPayload.Area> result = new LinkedHashMap<>();
        for (ActiveBeacon beacon : BeaconTracker.activeBeacons(level)) {
            BeaconFunction function = ModRegistries.FUNCTIONS.get(beacon.function());
            if (function != null && function.showsArea()) {
                result.put(beacon.pos(), new AreaSyncPayload.Area(beacon.pos(), beacon.area().radius()));
            }
        }
        Long2IntMap beams = BeamBeacons.of(level);
        long[] positions = beams.keySet().toLongArray();
        Arrays.sort(positions);
        for (long packed : positions) {
            BlockPos pos = BlockPos.of(packed);
            int radius = BeaconholdConfig.SERVER.radius(beams.get(packed));
            result.putIfAbsent(pos, new AreaSyncPayload.Area(pos, radius));
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Gives the areas that the server sends to a player at a position.
     *
     * @param level the dimension
     * @param x the X coordinate of the player
     * @param z the Z coordinate of the player
     * @return the areas whose edge is near the position, at most {@link AreaSyncPayload#MAX_AREAS}
     */
    public static List<AreaSyncPayload.Area> areasNear(ServerLevel level, double x, double z) {
        return near(areas(level), x, z);
    }

    private static List<AreaSyncPayload.Area> near(List<AreaSyncPayload.Area> areas, double x, double z) {
        List<AreaSyncPayload.Area> result = new ArrayList<>();
        for (AreaSyncPayload.Area area : areas) {
            if (result.size() < AreaSyncPayload.MAX_AREAS
                    && AreaVisibility.isNear(area.center().getX(), area.center().getZ(), area.radius(), x, z)) {
                result.add(area);
            }
        }
        return List.copyOf(result);
    }
}
