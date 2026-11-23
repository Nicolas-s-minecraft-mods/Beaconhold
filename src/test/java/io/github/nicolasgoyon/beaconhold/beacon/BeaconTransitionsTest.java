package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class BeaconTransitionsTest {
    private static final BlockPos POS = new BlockPos(1, 2, 3);
    private static final ResourceLocation A = ResourceLocation.fromNamespaceAndPath("beaconhold", "a");
    private static final ResourceLocation B = ResourceLocation.fromNamespaceAndPath("beaconhold", "b");

    private static BeaconRecord record(int level, boolean active, ResourceLocation function) {
        return new BeaconRecord(POS, level, active, function);
    }

    @Test
    void noChangeGivesNoEvent() {
        BeaconRecord beacon = record(2, true, A);
        BeaconTransitions.Transition transition = BeaconTransitions.of(beacon, record(2, true, A));
        assertNull(transition.stop());
        assertNull(transition.start());
    }

    @Test
    void newActiveFunctionStarts() {
        BeaconRecord after = record(1, true, A);
        BeaconTransitions.Transition transition = BeaconTransitions.of(null, after);
        assertNull(transition.stop());
        assertEquals(after, transition.start());
    }

    @Test
    void newInactiveFunctionDoesNotStart() {
        BeaconTransitions.Transition transition = BeaconTransitions.of(null, record(0, false, A));
        assertNull(transition.stop());
        assertNull(transition.start());
    }

    @Test
    void blockedBeamStops() {
        BeaconRecord before = record(3, true, A);
        BeaconTransitions.Transition transition = BeaconTransitions.of(before, record(3, false, A));
        assertEquals(before, transition.stop());
        assertNull(transition.start());
    }

    @Test
    void removedFunctionStops() {
        BeaconRecord before = record(3, true, A);
        BeaconTransitions.Transition transition = BeaconTransitions.of(before, null);
        assertEquals(before, transition.stop());
        assertNull(transition.start());
    }

    @Test
    void removedInactiveFunctionGivesNoEvent() {
        BeaconTransitions.Transition transition = BeaconTransitions.of(record(3, false, A), null);
        assertNull(transition.stop());
        assertNull(transition.start());
    }

    @Test
    void newPyramidLevelStopsThenStarts() {
        BeaconRecord before = record(1, true, A);
        BeaconRecord after = record(4, true, A);
        BeaconTransitions.Transition transition = BeaconTransitions.of(before, after);
        assertEquals(before, transition.stop());
        assertEquals(after, transition.start());
    }

    @Test
    void newFunctionStopsThenStarts() {
        BeaconRecord before = record(2, true, A);
        BeaconRecord after = record(2, true, B);
        BeaconTransitions.Transition transition = BeaconTransitions.of(before, after);
        assertEquals(before, transition.stop());
        assertEquals(after, transition.start());
    }

    @Test
    void newPyramidLevelOfAnInactiveBeaconGivesNoEvent() {
        BeaconTransitions.Transition transition = BeaconTransitions.of(record(1, false, A), record(2, false, A));
        assertNull(transition.stop());
        assertNull(transition.start());
    }
}
