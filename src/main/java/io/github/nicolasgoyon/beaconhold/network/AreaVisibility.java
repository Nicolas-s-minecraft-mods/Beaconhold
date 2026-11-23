package io.github.nicolasgoyon.beaconhold.network;

import io.github.nicolasgoyon.beaconhold.client.dome.DomeGeometry;

/**
 * The rule that tells which areas the server sends to a player. The client shows a dome only up to
 * {@link DomeGeometry#MAX_DISTANCE} blocks from its edge. The server sends the areas whose edge is near the player.
 * This class uses no class of the game.
 */
public final class AreaVisibility {
    /** The interval in ticks between two checks of the areas near each player. */
    public static final int CHECK_INTERVAL = 20;

    /**
     * The additional distance in blocks for the movement of the player between two checks. A player that flies with
     * elytra goes about 35 blocks in one second.
     */
    public static final double SEND_MARGIN = 64.0;

    /** The maximum horizontal distance in blocks between the player and the edge of an area that the server sends. */
    public static final double SEND_DISTANCE = DomeGeometry.MAX_DISTANCE + SEND_MARGIN;

    private AreaVisibility() {
    }

    /**
     * Tells if the edge of an area is near a player.
     *
     * @param centerX the X coordinate of the beacon block
     * @param centerZ the Z coordinate of the beacon block
     * @param radius the radius of the area in blocks
     * @param playerX the X coordinate of the player
     * @param playerZ the Z coordinate of the player
     * @return true if the horizontal distance between the player and the edge is {@link #SEND_DISTANCE} or less
     */
    public static boolean isNear(int centerX, int centerZ, int radius, double playerX, double playerZ) {
        double dx = playerX - (centerX + 0.5);
        double dz = playerZ - (centerZ + 0.5);
        double toEdge = Math.abs(Math.sqrt(dx * dx + dz * dz) - radius);
        return toEdge <= SEND_DISTANCE;
    }
}
