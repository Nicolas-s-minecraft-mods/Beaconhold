package io.github.nicolasgoyon.beaconhold.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class BeaconAreaTest {
    private static final BlockPos CENTER = new BlockPos(100, 64, -50);

    @Test
    void containsTheBeacon() {
        assertTrue(new BeaconArea(CENTER, 20).contains(CENTER));
    }

    @Test
    void containsAPositionOnTheEdge() {
        BeaconArea area = new BeaconArea(CENTER, 20);
        assertTrue(area.contains(CENTER.offset(20, 0, 0)));
        assertTrue(area.contains(CENTER.offset(0, 0, -20)));
        assertTrue(area.contains(CENTER.offset(12, 0, 16)));
    }

    @Test
    void doesNotContainAPositionOutOfTheRadius() {
        BeaconArea area = new BeaconArea(CENTER, 20);
        assertFalse(area.contains(CENTER.offset(21, 0, 0)));
        assertFalse(area.contains(CENTER.offset(15, 0, 15)));
    }

    @Test
    void isACylinderForTheFullHeight() {
        BeaconArea area = new BeaconArea(CENTER, 20);
        assertTrue(area.contains(new BlockPos(CENTER.getX() + 10, -64, CENTER.getZ())));
        assertTrue(area.contains(new BlockPos(CENTER.getX() + 10, 319, CENTER.getZ())));
    }

    @Test
    void radiusZeroContainsOnlyTheColumnOfTheBeacon() {
        BeaconArea area = new BeaconArea(CENTER, 0);
        assertTrue(area.contains(CENTER.above(30)));
        assertFalse(area.contains(CENTER.east()));
    }

    @Test
    void largeCoordinatesDoNotOverflow() {
        BlockPos far = new BlockPos(29_999_000, 64, -29_999_000);
        BeaconArea area = new BeaconArea(far, 100);
        assertTrue(area.contains(far.offset(-100, 0, 0)));
        assertFalse(area.contains(new BlockPos(-29_999_000, 64, 29_999_000)));
    }

    @Test
    void refusesANegativeRadius() {
        assertThrows(IllegalArgumentException.class, () -> new BeaconArea(CENTER, -1));
    }

    @Test
    void keepsAnImmutableCenter() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        BeaconArea area = new BeaconArea(mutable, 5);
        mutable.set(50, 50, 50);
        assertEquals(new BlockPos(1, 2, 3), area.center());
    }
}
