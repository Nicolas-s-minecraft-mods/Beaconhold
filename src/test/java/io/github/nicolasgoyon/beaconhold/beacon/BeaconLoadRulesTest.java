package io.github.nicolasgoyon.beaconhold.beacon;

import static io.github.nicolasgoyon.beaconhold.beacon.BeaconLoadRules.LOADED;
import static io.github.nicolasgoyon.beaconhold.beacon.BeaconLoadRules.READY;
import static io.github.nicolasgoyon.beaconhold.beacon.BeaconLoadRules.SCANNED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BeaconLoadRulesTest {
    @Test
    void aLoadedBeaconWaitsForItsScan() {
        assertEquals(LOADED, BeaconLoadRules.afterTick(LOADED, false, false));
        assertEquals(LOADED, BeaconLoadRules.afterTick(LOADED, true, false));
        assertEquals(SCANNED, BeaconLoadRules.afterTick(LOADED, false, true));
    }

    @Test
    void aScanThatEndsInAPeriodicTickWaitsForTheNextPeriodicTick() {
        // The game calculates the pyramid level before the end of the scan in the same tick.
        assertEquals(SCANNED, BeaconLoadRules.afterTick(LOADED, true, true));
        assertEquals(SCANNED, BeaconLoadRules.afterTick(SCANNED, false, true));
        assertEquals(READY, BeaconLoadRules.afterTick(SCANNED, true, false));
    }

    @Test
    void aReadyBeaconStaysReady() {
        assertEquals(READY, BeaconLoadRules.afterTick(READY, true, true));
        assertEquals(READY, BeaconLoadRules.afterTick(READY, false, false));
    }

    @Test
    void onlyAnActiveRecordStaysUntilTheBeaconIsReady() {
        assertTrue(BeaconLoadRules.keepsSavedState(LOADED, true));
        assertTrue(BeaconLoadRules.keepsSavedState(SCANNED, true));
        assertFalse(BeaconLoadRules.keepsSavedState(READY, true));
        assertFalse(BeaconLoadRules.keepsSavedState(LOADED, false));
    }
}
