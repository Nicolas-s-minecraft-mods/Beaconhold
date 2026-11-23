package io.github.nicolasgoyon.beaconhold.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CostRulesTest {
    @Test
    void defaultStackLastsTenGameDays() {
        assertEquals(240_000L, CostRules.duration(CostRules.STACK_SIZE, CostRules.DEFAULT_TICKS_PER_ITEM));
        assertEquals("10", CostRules.stackDays(CostRules.DEFAULT_TICKS_PER_ITEM));
    }

    @Test
    void defaultItemLastsThreeMinutesAndSevenSeconds() {
        assertEquals(3, CostRules.minutes(CostRules.DEFAULT_TICKS_PER_ITEM));
        assertEquals(7, CostRules.seconds(CostRules.DEFAULT_TICKS_PER_ITEM));
    }

    @Test
    void daysHaveOneDecimal() {
        assertEquals("2.5", CostRules.days(60_000L));
        assertEquals("0.1", CostRules.days(2_400L));
        assertEquals("0", CostRules.days(0L));
        assertEquals("0", CostRules.days(-100L));
    }

    @Test
    void negativeValuesGiveZero() {
        assertEquals(0, CostRules.minutes(-20));
        assertEquals(0, CostRules.seconds(-20));
        assertEquals(0L, CostRules.duration(-1, 3_750));
    }

    @Test
    void optionLimitsAreTheUpdateIntervalAndOneDay() {
        assertEquals(80, CostRules.MIN_TICKS_PER_ITEM);
        assertEquals(24_000, CostRules.MAX_TICKS_PER_ITEM);
    }

    @Test
    void comparatorSignalFollowsTheLevelOfTheStock() {
        assertEquals(0, CostRules.comparatorSignal(0, 64));
        assertEquals(1, CostRules.comparatorSignal(1, 64));
        assertEquals(8, CostRules.comparatorSignal(32, 64));
        assertEquals(14, CostRules.comparatorSignal(63, 64));
        assertEquals(15, CostRules.comparatorSignal(64, 64));
    }

    @Test
    void comparatorSignalStaysInItsRange() {
        assertEquals(15, CostRules.comparatorSignal(100, 64));
        assertEquals(0, CostRules.comparatorSignal(-3, 64));
        assertEquals(0, CostRules.comparatorSignal(5, 0));
        assertEquals(15, CostRules.comparatorSignal(16, 16));
    }
}
