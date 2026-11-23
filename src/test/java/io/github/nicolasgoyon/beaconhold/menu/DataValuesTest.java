package io.github.nicolasgoyon.beaconhold.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DataValuesTest {
    /** Gives the value that the client gets for a data slot: the game sends a 16-bit signed number. */
    private static int sent(int value) {
        return (short) value;
    }

    @Test
    void eachPartFitsInOneDataSlot() {
        for (int value : new int[] {0, 1, 512, 32_767, 32_768, 65_535, 100_000}) {
            assertTrue(DataValues.low(value) <= Short.MAX_VALUE, "Low part of " + value);
            assertTrue(DataValues.high(value) <= Short.MAX_VALUE, "High part of " + value);
        }
    }

    @Test
    void theClientGetsTheSameValue() {
        for (int value = 0; value <= 100_000; value += 7) {
            assertEquals(value, DataValues.join(sent(DataValues.low(value)), sent(DataValues.high(value))));
        }
        assertEquals(100_000, DataValues.join(sent(DataValues.low(100_000)), sent(DataValues.high(100_000))));
    }
}
