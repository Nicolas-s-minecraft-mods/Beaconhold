package io.github.nicolasgoyon.beaconhold.client.dome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DomeColorsTest {
    private static final float EPSILON = 1.0e-6F;

    @Test
    void validColorsHaveTheFormatHashAndSixHexDigits() {
        assertTrue(DomeColors.isValidColor("#9EE8FF"));
        assertTrue(DomeColors.isValidColor("#ff0000"));
        assertFalse(DomeColors.isValidColor("9EE8FF"));
        assertFalse(DomeColors.isValidColor("#9EE8F"));
        assertFalse(DomeColors.isValidColor("#GGGGGG"));
        assertFalse(DomeColors.isValidColor(42));
        assertFalse(DomeColors.isValidColor(null));
    }

    @Test
    void colorGivesItsParts() {
        int rgb = DomeColors.rgb("#FF8000");
        assertEquals(1.0F, DomeColors.red(rgb), EPSILON);
        assertEquals(128 / 255.0F, DomeColors.green(rgb), EPSILON);
        assertEquals(0.0F, DomeColors.blue(rgb), EPSILON);
    }

    @Test
    void invalidTextGivesTheDefaultColor() {
        assertEquals(DomeColors.rgb(DomeColors.DEFAULT_COLOR), DomeColors.rgb("blue"));
    }

    @Test
    void defaultColorIsTheLightCyanOfTheDome() {
        int rgb = DomeColors.rgb(DomeColors.DEFAULT_COLOR);
        assertEquals(0.62F, DomeColors.red(rgb), 0.005F);
        assertEquals(0.91F, DomeColors.green(rgb), 0.005F);
        assertEquals(1.0F, DomeColors.blue(rgb), EPSILON);
    }

    @Test
    void defaultOpacitiesGiveTheOpacitiesOfTheGeometry() {
        assertEquals(12, DomeColors.DEFAULT_VEIL_OPACITY);
        assertEquals(50, DomeColors.DEFAULT_RING_OPACITY);
        float veil = DomeColors.opacity(DomeGeometry.VEIL_ALPHA, DomeGeometry.VEIL_ALPHA, DomeColors.DEFAULT_VEIL_OPACITY);
        assertEquals((float) DomeGeometry.VEIL_ALPHA, veil, EPSILON);
        float ring = DomeColors.opacity(0.25, DomeGeometry.RING_ALPHA, DomeColors.DEFAULT_RING_OPACITY);
        assertEquals(0.25F, ring, EPSILON);
    }

    @Test
    void opacityFollowsTheOption() {
        assertEquals(0.0F, DomeColors.opacity(DomeGeometry.VEIL_ALPHA, DomeGeometry.VEIL_ALPHA, 0), EPSILON);
        assertEquals(1.0F, DomeColors.opacity(DomeGeometry.RING_ALPHA, DomeGeometry.RING_ALPHA, 100), EPSILON);
        assertEquals(0.5F, DomeColors.opacity(DomeGeometry.RING_ALPHA / 2, DomeGeometry.RING_ALPHA, 100), EPSILON);
    }

    @Test
    void opacityStaysInItsRange() {
        assertEquals(1.0F, DomeColors.opacity(DomeGeometry.RING_ALPHA, DomeGeometry.RING_ALPHA, 250), EPSILON);
        assertEquals(0.0F, DomeColors.opacity(DomeGeometry.RING_ALPHA, DomeGeometry.RING_ALPHA, -5), EPSILON);
        assertEquals(0.0F, DomeColors.opacity(0.3, 0.0, 50), EPSILON);
    }
}
