package io.github.nicolasgoyon.beaconhold.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nicolasgoyon.beaconhold.client.dome.DomeGeometry;
import org.junit.jupiter.api.Test;

class AreaVisibilityTest {
    private static final int X = 100;
    private static final int Z = -40;
    private static final int RADIUS = 47;

    /** The X coordinate of a player at a distance from the center of the beacon block, on the same Z line. */
    private static double at(double distance) {
        return X + 0.5 + distance;
    }

    @Test
    void sendDistanceCoversTheDistanceOfTheDomeAndAMargin() {
        assertEquals(DomeGeometry.MAX_DISTANCE + AreaVisibility.SEND_MARGIN, AreaVisibility.SEND_DISTANCE);
        assertTrue(AreaVisibility.SEND_MARGIN >= 35.0 * AreaVisibility.CHECK_INTERVAL / 20.0);
    }

    @Test
    void anAreaIsNearWhenItsEdgeIsNear() {
        double limit = AreaVisibility.SEND_DISTANCE;
        assertTrue(AreaVisibility.isNear(X, Z, RADIUS, at(0), Z + 0.5), "At the beacon");
        assertTrue(AreaVisibility.isNear(X, Z, RADIUS, at(RADIUS), Z + 0.5), "On the edge");
        assertTrue(AreaVisibility.isNear(X, Z, RADIUS, at(RADIUS + limit), Z + 0.5), "At the limit");
        assertFalse(AreaVisibility.isNear(X, Z, RADIUS, at(RADIUS + limit + 1), Z + 0.5), "Out of the limit");
        assertFalse(AreaVisibility.isNear(X, Z, RADIUS, X + 0.5, Z + 0.5 - RADIUS - limit - 1), "On the Z axis");
    }

    @Test
    void theCenterOfAVeryLargeAreaIsFarFromItsEdge() {
        int radius = (int) AreaVisibility.SEND_DISTANCE + 10;
        assertFalse(AreaVisibility.isNear(X, Z, radius, at(0), Z + 0.5));
        assertTrue(AreaVisibility.isNear(X, Z, radius, at(10), Z + 0.5));
    }
}
