package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nicolasgoyon.beaconhold.api.BeaconArea;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

class VanillaPowerAreaTest {
    private static final BlockPos BEACON = new BlockPos(-40, 70, 25);
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    @Test
    void searchBoxGoesFromTheBottomToTheTopOfTheWorld() {
        AABB box = VanillaPowerArea.searchBox(BEACON, 38, MIN_Y, MAX_Y);
        assertEquals(MIN_Y, box.minY);
        assertEquals(MAX_Y, box.maxY);
    }

    @Test
    void searchBoxContainsEachColumnOfTheArea() {
        int radius = 38;
        AABB box = VanillaPowerArea.searchBox(BEACON, radius, MIN_Y, MAX_Y);
        BeaconArea area = new BeaconArea(BEACON, radius);
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                int x = BEACON.getX() + dx;
                int z = BEACON.getZ() + dz;
                if (area.containsColumn(x, z)) {
                    // A player in the column has its position in the block, from x to x + 1.
                    assertTrue(box.contains(x + 0.5, 0, z + 0.5), "Column " + dx + ", " + dz);
                    assertTrue(box.contains(x, 0, z), "Column " + dx + ", " + dz);
                }
            }
        }
    }

    @Test
    void searchBoxHasNoColumnOutOfTheSquareOfTheArea() {
        AABB box = VanillaPowerArea.searchBox(BEACON, 38, MIN_Y, MAX_Y);
        assertFalse(box.contains(BEACON.getX() + 39.5, 0, BEACON.getZ() + 0.5));
        assertFalse(box.contains(BEACON.getX() - 38.5, 0, BEACON.getZ() + 0.5));
    }

    @Test
    void areaIsACylinderAndNotTheVanillaSquare() {
        // The vanilla range at the pyramid level 2 is a square of 30 blocks around the beacon. The default area is a
        // circle of 38 blocks.
        BeaconArea area = new BeaconArea(BEACON, 38);
        assertTrue(area.contains(BEACON.offset(35, -100, 0)), "Out of the vanilla range, in the area");
        assertFalse(area.contains(BEACON.offset(27, 0, 27)), "In the vanilla range, out of the area");
    }
}
