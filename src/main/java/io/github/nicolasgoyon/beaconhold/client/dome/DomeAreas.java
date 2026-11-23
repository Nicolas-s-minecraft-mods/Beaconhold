package io.github.nicolasgoyon.beaconhold.client.dome;

import io.github.nicolasgoyon.beaconhold.network.AreaSyncPayload;
import java.util.List;

/**
 * The areas that the client shows as energy domes. The last payload of the server gives the areas. This class uses no
 * class of the client, so the handler of the payload can use it on the two sides.
 */
public final class DomeAreas {
    private static volatile List<AreaSyncPayload.Area> areas = List.of();

    private DomeAreas() {
    }

    /**
     * Gives the areas of the dimension of the player.
     *
     * @return the areas. The list cannot change.
     */
    public static List<AreaSyncPayload.Area> get() {
        return areas;
    }

    /**
     * Replaces the areas.
     *
     * @param newAreas the areas of the dimension of the player
     */
    public static void set(List<AreaSyncPayload.Area> newAreas) {
        areas = List.copyOf(newAreas);
    }

    /** Removes all the areas, for example when the player leaves the server. */
    public static void clear() {
        areas = List.of();
    }
}
