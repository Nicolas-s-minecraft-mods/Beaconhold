package io.github.nicolasgoyon.beaconhold.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AreaRadiiTest {
    @Test
    void defaultRadiiGoFrom20To75() {
        assertArrayEquals(new int[] {20, 38, 57, 75}, AreaRadii.defaultRadii());
    }

    @Test
    void eachPyramidLevelHasItsRadius() {
        int[] radii = AreaRadii.defaultRadii();
        assertEquals(20, AreaRadii.radius(1, radii));
        assertEquals(38, AreaRadii.radius(2, radii));
        assertEquals(57, AreaRadii.radius(3, radii));
        assertEquals(75, AreaRadii.radius(4, radii));
    }

    @Test
    void pyramidLevelZeroHasNoArea() {
        assertEquals(0, AreaRadii.radius(0, AreaRadii.defaultRadii()));
        assertEquals(0, AreaRadii.radius(-1, AreaRadii.defaultRadii()));
    }

    @Test
    void levelAboveTheLastLevelUsesTheLastRadius() {
        assertEquals(75, AreaRadii.radius(5, AreaRadii.defaultRadii()));
    }

    @Test
    void noRadiusGivesNoArea() {
        assertEquals(0, AreaRadii.radius(2, new int[0]));
    }

    @Test
    void defaultRadiiIsACopy() {
        AreaRadii.defaultRadii()[0] = 999;
        assertEquals(20, AreaRadii.defaultRadii()[0]);
    }
}
