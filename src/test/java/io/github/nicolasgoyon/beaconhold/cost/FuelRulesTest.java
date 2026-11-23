package io.github.nicolasgoyon.beaconhold.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FuelRulesTest {
    private static final int INTERVAL = 80;

    /** Runs the updates until the function stops, and gives the ticks from the start to the stop. */
    private static long operatingTicks(int stock, int ticksPerItem) {
        FuelRules.State state = FuelRules.update(stock, 0, ticksPerItem, 0, true);
        long ticks = 0;
        while (state.active()) {
            state = FuelRules.update(state.stock(), state.fuel(), ticksPerItem, INTERVAL, true);
            ticks += INTERVAL;
        }
        return ticks;
    }

    @Test
    void oneStackLastsTenGameDays() {
        assertEquals(240_000L, operatingTicks(CostRules.STACK_SIZE, CostRules.DEFAULT_TICKS_PER_ITEM));
    }

    @Test
    void periodThatIsNotAMultipleOfTheIntervalKeepsTheAverage() {
        // 1000 ticks for each item: 64 items give 64,000 ticks, that is 800 updates.
        assertEquals(64_000L, operatingTicks(64, 1_000));
    }

    @Test
    void startTakesOneItem() {
        FuelRules.State state = FuelRules.update(10, 0, 3_750, 0, true);
        assertEquals(new FuelRules.State(9, 3_750, true), state);
    }

    @Test
    void updateUsesTheFuelBeforeTheStock() {
        FuelRules.State state = FuelRules.update(10, 200, 3_750, 80, true);
        assertEquals(new FuelRules.State(10, 120, true), state);
    }

    @Test
    void debtOfFuelGoesToTheNextItem() {
        FuelRules.State state = FuelRules.update(10, 50, 100, 80, true);
        assertEquals(new FuelRules.State(9, 70, true), state);
    }

    @Test
    void noStockAndNoFuelStopsTheFunction() {
        assertEquals(new FuelRules.State(0, 0, false), FuelRules.update(0, 80, 3_750, 80, true));
        assertEquals(new FuelRules.State(0, -40, false), FuelRules.update(0, 40, 3_750, 80, true));
    }

    @Test
    void beaconThatCannotOperateKeepsItsStock() {
        FuelRules.State state = FuelRules.update(10, 50, 3_750, 0, false);
        assertEquals(new FuelRules.State(10, 50, false), state);
    }

    @Test
    void beaconThatStopsPaysTheTimeOfOperation() {
        FuelRules.State state = FuelRules.update(10, 50, 3_750, 30, false);
        assertEquals(new FuelRules.State(10, 20, false), state);
    }

    @Test
    void elapsedTicksCountOnlyForAnActiveFunction() {
        assertEquals(0, FuelRules.elapsed(1_000, 920, false));
        assertEquals(80, FuelRules.elapsed(1_000, 920, true));
        assertEquals(35, FuelRules.elapsed(1_000, 965, true));
    }

    @Test
    void elapsedTicksHaveALimit() {
        // A beacon in a chunk that is not loaded does not operate.
        assertEquals(FuelRules.MAX_ELAPSED, FuelRules.elapsed(1_000_000, 0, true));
        assertEquals(0, FuelRules.elapsed(100, 200, true));
    }

    @Test
    void itemsDoNotStackUpWhenTheFunctionStartsAgain() {
        FuelRules.State first = FuelRules.update(10, 0, 3_750, 0, true);
        FuelRules.State stopped = FuelRules.update(first.stock(), first.fuel(), 3_750, 10, false);
        FuelRules.State again = FuelRules.update(stopped.stock(), stopped.fuel(), 3_750, 0, true);
        assertEquals(new FuelRules.State(9, 3_740, true), again);
    }

    @Test
    void remainingTimeAddsTheStockAndTheFuel() {
        assertEquals(2L * 3_750 + 100, FuelRules.remainingTicks(2, 100, 3_750));
        assertEquals(0L, FuelRules.remainingTicks(0, -20, 3_750));
    }

    @Test
    void stockIsLowUnderOneGameDay() {
        assertTrue(FuelRules.isLow(0));
        assertTrue(FuelRules.isLow(23_999));
        assertFalse(FuelRules.isLow(24_000));
        // With the default cost and the fuel of one item, 5 items give less than one day. 6 items give more.
        assertTrue(FuelRules.isLow(FuelRules.remainingTicks(5, 3_750, 3_750)));
        assertFalse(FuelRules.isLow(FuelRules.remainingTicks(6, 3_750, 3_750)));
    }
}
