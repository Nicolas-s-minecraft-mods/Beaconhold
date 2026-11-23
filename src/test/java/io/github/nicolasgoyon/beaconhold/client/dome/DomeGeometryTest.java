package io.github.nicolasgoyon.beaconhold.client.dome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DomeGeometryTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void domeIsHalfABlockOutsideTheAreaRadius() {
        assertEquals(20.5, DomeGeometry.edgeRadius(20), EPSILON);
    }

    @Test
    void segmentsHaveAboutFourBlocks() {
        // 2 * pi * 47.5 / 4 = 74.6
        assertEquals(75, DomeGeometry.segmentCount(47));
    }

    @Test
    void segmentCountHasLimits() {
        assertEquals(DomeGeometry.MIN_SEGMENTS, DomeGeometry.segmentCount(0));
        assertEquals(DomeGeometry.MIN_SEGMENTS, DomeGeometry.segmentCount(-5));
        assertEquals(DomeGeometry.MAX_SEGMENTS, DomeGeometry.segmentCount(512));
    }

    @Test
    void pointsAreOnTheCircle() {
        int count = DomeGeometry.segmentCount(100);
        for (int i = 0; i <= count; i++) {
            double x = DomeGeometry.pointX(10.5, 100.5, i, count);
            double z = DomeGeometry.pointZ(-3.5, 100.5, i, count);
            assertEquals(100.5, DomeGeometry.horizontalDistance(10.5, -3.5, x, z), 1.0e-6);
        }
    }

    @Test
    void lastPointClosesTheCircle() {
        int count = DomeGeometry.segmentCount(20);
        assertEquals(DomeGeometry.pointX(0, 20.5, 0, count), DomeGeometry.pointX(0, 20.5, count, count), EPSILON);
        assertEquals(DomeGeometry.pointZ(0, 20.5, 0, count), DomeGeometry.pointZ(0, 20.5, count, count), EPSILON);
    }

    @Test
    void veilHasNoLightNearThePlayer() {
        assertEquals(0.0, DomeGeometry.veilAlpha(0.0), EPSILON);
        assertEquals(0.0, DomeGeometry.veilAlpha(DomeGeometry.NEAR_DISTANCE), EPSILON);
        assertTrue(DomeGeometry.veilAlpha(20.0) > 0.0);
        assertTrue(DomeGeometry.veilAlpha(20.0) < DomeGeometry.VEIL_ALPHA);
    }

    @Test
    void veilIsFaintFromFar() {
        assertEquals(DomeGeometry.VEIL_ALPHA, DomeGeometry.veilAlpha(100.0), EPSILON);
        assertTrue(DomeGeometry.VEIL_ALPHA <= 0.2, "The veil is faint");
    }

    @Test
    void veilFadesWithTheDistance() {
        assertTrue(DomeGeometry.veilAlpha(200.0) < DomeGeometry.VEIL_ALPHA);
        assertTrue(DomeGeometry.veilAlpha(240.0) < DomeGeometry.veilAlpha(200.0));
        assertEquals(0.0, DomeGeometry.veilAlpha(DomeGeometry.MAX_DISTANCE), EPSILON);
        assertEquals(0.0, DomeGeometry.veilAlpha(1000.0), EPSILON);
    }

    @Test
    void ringShowsOnlyNearThePlayer() {
        assertEquals(DomeGeometry.RING_ALPHA, DomeGeometry.ringAlpha(0.0), EPSILON);
        assertEquals(DomeGeometry.RING_ALPHA, DomeGeometry.ringAlpha(DomeGeometry.NEAR_DISTANCE), EPSILON);
        assertEquals(DomeGeometry.RING_ALPHA / 2, DomeGeometry.ringAlpha(16.0), EPSILON);
        assertEquals(0.0, DomeGeometry.ringAlpha(DomeGeometry.RING_DISTANCE), EPSILON);
        assertEquals(0.0, DomeGeometry.ringAlpha(100.0), EPSILON);
    }

    @Test
    void particlesFollowTheRing() {
        assertEquals(DomeGeometry.PARTICLE_CHANCE, DomeGeometry.particleChance(0.0), EPSILON);
        assertEquals(0.0, DomeGeometry.particleChance(DomeGeometry.RING_DISTANCE), EPSILON);
    }

    @Test
    void domeIsInRangeNearItsEdge() {
        assertTrue(DomeGeometry.isInRange(0, 0, 0, 0, 100.5), "Inside the area, 100 blocks from the edge");
        assertTrue(DomeGeometry.isInRange(300, 0, 0, 0, 100.5), "Outside the area, 200 blocks from the edge");
        assertFalse(DomeGeometry.isInRange(400, 0, 0, 0, 100.5), "Outside the area, 300 blocks from the edge");
        assertFalse(DomeGeometry.isInRange(0, 0, 0, 0, 300.5), "Inside a large area, 300 blocks from the edge");
    }

    @Test
    void ringIsOnTheGround() {
        assertEquals(64.0, DomeGeometry.ringY(64, 64.0), EPSILON);
        assertEquals(64.0, DomeGeometry.ringY(64, 80.0), EPSILON);
        assertEquals(64.0, DomeGeometry.ringY(64, 58.0), EPSILON);
    }

    @Test
    void ringIsAtTheFeetOfAPlayerInACave() {
        assertEquals(30.0, DomeGeometry.ringY(64, 30.4), EPSILON);
    }
}
