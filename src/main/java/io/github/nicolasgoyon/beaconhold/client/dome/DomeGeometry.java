package io.github.nicolasgoyon.beaconhold.client.dome;

/**
 * The rules of the shape and of the fade of the energy dome. This class uses no class of the game.
 *
 * <p>The dome is on the edge of the area: a vertical circle around the beacon. From far, the dome is a faint veil. Near
 * the edge, the veil fades out, and the dome shows a ring of light on the ground with particles. Far from the edge, the
 * veil also fades out.
 */
public final class DomeGeometry {
    /** The length in blocks of one segment of the circle. */
    static final double SEGMENT_LENGTH = 4.0;

    /** The minimum number of segments of the circle. */
    static final int MIN_SEGMENTS = 24;

    /** The maximum number of segments of the circle. */
    static final int MAX_SEGMENTS = 256;

    /** The distance in blocks where the ring has its full light, and the veil has no light. */
    public static final double NEAR_DISTANCE = 8.0;

    /** The distance in blocks where the ring has no light. */
    public static final double RING_DISTANCE = 24.0;

    /** The distance in blocks where the veil gets its full light. */
    static final double VEIL_FULL_DISTANCE = 32.0;

    /** The distance in blocks where the veil starts to fade out. */
    static final double FADE_START_DISTANCE = 160.0;

    /** The distance in blocks where the veil has no light. The client does not show the dome farther. */
    public static final double MAX_DISTANCE = 256.0;

    /** The opacity of the veil, from 0 to 1. */
    public static final double VEIL_ALPHA = 0.12;

    /** The opacity of the ring, from 0 to 1. */
    public static final double RING_ALPHA = 0.5;

    /** The half height of the veil in blocks, above and below the eyes of the player. */
    public static final double VEIL_HALF_HEIGHT = 96.0;

    /** The height of the ring in blocks. */
    public static final double RING_HEIGHT = 1.5;

    /** The depth in blocks under the ground where the ring goes to the level of the feet of the player. */
    static final int CAVE_DEPTH = 6;

    /** The chance for each segment and each tick to make a particle, at the full light of the ring. */
    static final double PARTICLE_CHANCE = 0.08;

    private DomeGeometry() {
    }

    /**
     * Gives the radius of the circle of the dome. A block is in the area when its center is in the circle of the area
     * radius, so the dome is half a block outside this circle.
     *
     * @param radius the radius of the area in blocks
     * @return the radius of the dome in blocks, from the center of the beacon block
     */
    public static double edgeRadius(int radius) {
        return radius + 0.5;
    }

    /**
     * Gives the number of segments of the circle.
     *
     * @param radius the radius of the area in blocks
     * @return a number from {@link #MIN_SEGMENTS} to {@link #MAX_SEGMENTS}
     */
    public static int segmentCount(int radius) {
        double circumference = 2.0 * Math.PI * edgeRadius(Math.max(0, radius));
        int count = (int) Math.ceil(circumference / SEGMENT_LENGTH);
        return Math.max(MIN_SEGMENTS, Math.min(MAX_SEGMENTS, count));
    }

    /**
     * Gives the X coordinate of a point of the circle.
     *
     * @param centerX the X coordinate of the center
     * @param edgeRadius the radius of the circle
     * @param index the index of the point, from 0 to {@code count}
     * @param count the number of segments
     * @return the X coordinate
     */
    public static double pointX(double centerX, double edgeRadius, int index, int count) {
        return centerX + edgeRadius * Math.cos(angle(index, count));
    }

    /**
     * Gives the Z coordinate of a point of the circle.
     *
     * @param centerZ the Z coordinate of the center
     * @param edgeRadius the radius of the circle
     * @param index the index of the point, from 0 to {@code count}
     * @param count the number of segments
     * @return the Z coordinate
     */
    public static double pointZ(double centerZ, double edgeRadius, int index, int count) {
        return centerZ + edgeRadius * Math.sin(angle(index, count));
    }

    private static double angle(int index, int count) {
        return 2.0 * Math.PI * (index % count) / count;
    }

    /**
     * Gives the horizontal distance between two points.
     *
     * @param x1 the X coordinate of the first point
     * @param z1 the Z coordinate of the first point
     * @param x2 the X coordinate of the second point
     * @param z2 the Z coordinate of the second point
     * @return the distance in blocks
     */
    public static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Tells if a part of the dome is near enough to show.
     *
     * @param cameraX the X coordinate of the camera
     * @param cameraZ the Z coordinate of the camera
     * @param centerX the X coordinate of the center of the dome
     * @param centerZ the Z coordinate of the center of the dome
     * @param edgeRadius the radius of the dome
     * @return true if the horizontal distance from the camera to the nearest point of the dome is less than
     *     {@link #MAX_DISTANCE}
     */
    public static boolean isInRange(double cameraX, double cameraZ, double centerX, double centerZ, double edgeRadius) {
        double toCenter = horizontalDistance(cameraX, cameraZ, centerX, centerZ);
        return Math.abs(toCenter - edgeRadius) < MAX_DISTANCE;
    }

    /**
     * Gives the opacity of the veil at a point of the dome.
     *
     * @param distance the horizontal distance from the camera to the point
     * @return 0 near the point and far from the point, {@link #VEIL_ALPHA} between
     */
    public static double veilAlpha(double distance) {
        double fadeIn = ramp(distance, NEAR_DISTANCE, VEIL_FULL_DISTANCE);
        double fadeOut = 1.0 - ramp(distance, FADE_START_DISTANCE, MAX_DISTANCE);
        return VEIL_ALPHA * fadeIn * fadeOut;
    }

    /**
     * Gives the opacity of the ring at a point of the dome.
     *
     * @param distance the horizontal distance from the camera to the point
     * @return {@link #RING_ALPHA} near the point, then less, and 0 at {@link #RING_DISTANCE} and farther
     */
    public static double ringAlpha(double distance) {
        return RING_ALPHA * (1.0 - ramp(distance, NEAR_DISTANCE, RING_DISTANCE));
    }

    /**
     * Gives the chance to make a particle at a segment of the ring, for one tick.
     *
     * @param distance the horizontal distance from the camera to the segment
     * @return a chance from 0 to {@link #PARTICLE_CHANCE}
     */
    public static double particleChance(double distance) {
        return PARTICLE_CHANCE * ringAlpha(distance) / RING_ALPHA;
    }

    /**
     * Gives the height of the bottom of the ring at a point.
     *
     * @param groundY the height of the first block of air above the ground at the point
     * @param feetY the height of the feet of the player
     * @return the ground height. In a cave, more than {@link #CAVE_DEPTH} blocks under the ground, the height of the
     *     feet of the player.
     */
    public static double ringY(int groundY, double feetY) {
        return feetY < groundY - CAVE_DEPTH ? Math.floor(feetY) : groundY;
    }

    /** Gives 0 at {@code start} and before, 1 at {@code end} and after, and a linear value between. */
    private static double ramp(double value, double start, double end) {
        if (value <= start) {
            return 0.0;
        }
        if (value >= end) {
            return 1.0;
        }
        return (value - start) / (end - start);
    }
}
