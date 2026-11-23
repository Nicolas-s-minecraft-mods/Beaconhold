package io.github.nicolasgoyon.beaconhold.beacon;

import javax.annotation.Nullable;

/** The rule that tells if an active beacon shows the energy dome. */
public final class DomeRules {
    private DomeRules() {
    }

    /**
     * Tells if a loaded beacon shows the energy dome of its area.
     *
     * <p>The beacon is active when the pyramid level is 1 or more, no block stops the beam and the beacon has fuel or a
     * stock of the consumable.
     *
     * @param pyramidLevel the pyramid level of the beacon
     * @param beamFree true if no block stops the beam
     * @param on true if the beacon has fuel or a stock of the consumable (see {@link BeaconPower})
     * @param functionShowsArea the value of {@code showsArea} of the function of the beacon, or {@code null} for a
     *     beacon with no function
     * @param vanillaPowersUseArea the value of the server option {@code area.vanillaPowersUseArea}
     * @return true if the beacon is active and the function does not hide the area. A beacon with no function shows the
     *     dome only when its vanilla powers use the area.
     */
    public static boolean showsDome(
            int pyramidLevel,
            boolean beamFree,
            boolean on,
            @Nullable Boolean functionShowsArea,
            boolean vanillaPowersUseArea) {
        if (pyramidLevel <= 0 || !beamFree || !on) {
            return false;
        }
        if (functionShowsArea == null) {
            return vanillaPowersUseArea;
        }
        return functionShowsArea;
    }
}
