package io.github.nicolasgoyon.beaconhold.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WarningRulesTest {
    @Test
    void stockUnderOneGameDayIsLow() {
        assertTrue(WarningRules.isLow(CostRules.TICKS_PER_DAY - 1));
        assertFalse(WarningRules.isLow(CostRules.TICKS_PER_DAY));
        assertTrue(WarningRules.isLow(0));
    }

    @Test
    void firstWarningShows() {
        assertTrue(WarningRules.shouldWarn(null, 1_000));
    }

    @Test
    void warningWaitsForTheCooldown() {
        long last = 10_000;
        assertFalse(WarningRules.shouldWarn(last, last));
        assertFalse(WarningRules.shouldWarn(last, last + WarningRules.COOLDOWN_TICKS - 1));
        assertTrue(WarningRules.shouldWarn(last, last + WarningRules.COOLDOWN_TICKS));
    }

    @Test
    void cooldownIsFiveMinutes() {
        assertEquals(6_000, WarningRules.COOLDOWN_TICKS);
    }

    @Test
    void earlierTickOfANewWorldShowsTheWarning() {
        assertTrue(WarningRules.shouldWarn(50_000L, 100));
    }
}
