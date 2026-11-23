package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PowerRulesTest {
    @Test
    void beaconThatCanOperateWithNoFuelIsOff() {
        assertTrue(PowerRules.isOffWithNoConsumable(true, false));
    }

    @Test
    void beaconWithFuelIsNotOff() {
        assertFalse(PowerRules.isOffWithNoConsumable(true, true));
    }

    @Test
    void beaconThatCannotOperateIsOffForAnotherCause() {
        assertFalse(PowerRules.isOffWithNoConsumable(false, false));
    }

    @Test
    void bonusAddsToTheVanillaAmplifier() {
        assertEquals(1, PowerRules.amplifier(0, 1));
        assertEquals(2, PowerRules.amplifier(1, 1));
        assertEquals(0, PowerRules.amplifier(0, 0));
        assertEquals(1, PowerRules.amplifier(1, -3));
        assertEquals(1, PowerRules.amplifier(0, PowerRules.DEFAULT_LEVEL_BONUS));
    }

    @Test
    void withTheConsumableRequiredTheBeaconNeedsFuel() {
        assertTrue(PowerRules.isOn(true, true, true));
        assertFalse(PowerRules.isOn(true, true, false));
        assertFalse(PowerRules.isOn(true, false, true));
    }

    @Test
    void withNoConsumableRequiredTheVanillaRuleApplies() {
        assertTrue(PowerRules.isOn(false, true, false));
        assertTrue(PowerRules.isOn(false, true, true));
        assertFalse(PowerRules.isOn(false, false, false));
    }
}
