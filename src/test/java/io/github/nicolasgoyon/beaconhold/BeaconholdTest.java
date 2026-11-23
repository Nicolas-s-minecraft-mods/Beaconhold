package io.github.nicolasgoyon.beaconhold;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BeaconholdTest {
    @Test
    void modIdIsBeaconhold() {
        assertEquals("beaconhold", Beaconhold.MOD_ID);
    }
}
