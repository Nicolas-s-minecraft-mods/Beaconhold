package io.github.nicolasgoyon.beaconhold.config;

/**
 * The rule that gives the radius of the area of a beacon from its pyramid level. The functions and the vanilla powers of
 * the beacon use this area.
 */
public final class AreaRadii {
    /** The default radii in blocks, for the pyramid levels 1 to 4. */
    static final int[] DEFAULT_RADII = {20, 38, 57, 75};

    /** The maximum radius in blocks that the options accept. */
    static final int MAX_RADIUS = 512;

    private AreaRadii() {
    }

    /**
     * Gives the radius of the area for a pyramid level.
     *
     * @param pyramidLevel the pyramid level
     * @param radii the radii for the pyramid levels 1 to {@code radii.length}
     * @return 0 for a pyramid level of 0 or less. The last radius for a pyramid level above the last level.
     */
    public static int radius(int pyramidLevel, int[] radii) {
        if (pyramidLevel <= 0 || radii.length == 0) {
            return 0;
        }
        return radii[Math.min(pyramidLevel, radii.length) - 1];
    }

    /**
     * Tells if a change of the radii changes the area of a beacon.
     *
     * @param pyramidLevel the pyramid level of the beacon
     * @param before the radii before the change, for the pyramid levels 1 to {@code before.length}
     * @param after the radii after the change, for the pyramid levels 1 to {@code after.length}
     * @return true if the radius of the pyramid level is different
     */
    public static boolean radiusChanged(int pyramidLevel, int[] before, int[] after) {
        return radius(pyramidLevel, before) != radius(pyramidLevel, after);
    }

    /**
     * Gives a copy of the default radii.
     *
     * @return the default radii in blocks, for the pyramid levels 1 to 4
     */
    public static int[] defaultRadii() {
        return DEFAULT_RADII.clone();
    }
}
