package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DomeRulesTest {
    @Test
    void activeBeaconWithNoFunctionShowsTheDome() {
        assertTrue(DomeRules.showsDome(1, true, true, null, true));
        assertTrue(DomeRules.showsDome(4, true, true, null, true));
    }

    @Test
    void noPyramidOrABlockedBeamGivesNoDome() {
        assertFalse(DomeRules.showsDome(0, true, true, null, true));
        assertFalse(DomeRules.showsDome(2, false, true, null, true));
        assertFalse(DomeRules.showsDome(0, true, true, true, true));
        assertFalse(DomeRules.showsDome(2, false, true, true, true));
    }

    @Test
    void beaconWithNoConsumableGivesNoDome() {
        assertFalse(DomeRules.showsDome(2, true, false, null, true));
        assertFalse(DomeRules.showsDome(2, true, false, true, true));
    }

    @Test
    void functionThatHidesItsAreaGivesNoDome() {
        assertFalse(DomeRules.showsDome(2, true, true, false, true));
        assertFalse(DomeRules.showsDome(2, true, true, false, false));
    }

    @Test
    void functionThatShowsItsAreaShowsTheDomeWithTheTwoValuesOfTheOption() {
        assertTrue(DomeRules.showsDome(2, true, true, true, true));
        assertTrue(DomeRules.showsDome(2, true, true, true, false));
    }

    @Test
    void beaconWithNoFunctionShowsNoDomeWhenThePowersUseTheVanillaRange() {
        assertFalse(DomeRules.showsDome(2, true, true, null, false));
    }
}
