package io.github.nicolasgoyon.beaconhold.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AreaIndexTest {
    @Test
    void areaContainsItsEdgeAndNotTheNextBlock() {
        AreaIndex<String> index = new AreaIndex<>();
        index.put(1L, 10, -20, 38, "a");
        assertEquals(List.of("a"), index.containing(48, -20));
        assertEquals(List.of(), index.containing(49, -20));
        assertEquals(List.of("a"), index.containing(10, -58));
        assertEquals(List.of(), index.containing(37, 7));
    }

    @Test
    void areaOnTheBorderOfTheCellsIsInEachCell() {
        AreaIndex<String> index = new AreaIndex<>();
        index.put(1L, 0, 0, 20, "a");
        assertEquals(List.of("a"), index.containing(-20, 0));
        assertEquals(List.of("a"), index.containing(20, 0));
        assertEquals(List.of("a"), index.containing(0, -20));
    }

    @Test
    void putReplacesAndRemoveRemoves() {
        AreaIndex<String> index = new AreaIndex<>();
        index.put(7L, 0, 0, 20, "a");
        index.put(7L, 500, 500, 20, "b");
        assertEquals(1, index.size());
        assertEquals(List.of(), index.containing(0, 0));
        assertEquals(List.of("b"), index.containing(500, 500));
        index.remove(7L);
        assertEquals(0, index.size());
        assertEquals(List.of(), index.containing(500, 500));
        index.remove(7L);
        assertEquals(0, index.size());
    }

    @Test
    void indexGivesTheSameAreasAsAFullScan() {
        Random random = new Random(17);
        AreaIndex<Integer> index = new AreaIndex<>();
        List<int[]> areas = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            int[] area = {random.nextInt(4001) - 2000, random.nextInt(4001) - 2000, 1 + random.nextInt(120)};
            areas.add(area);
            index.put(i, area[0], area[1], area[2], i);
        }
        for (int i = 0; i < 100; i += 3) {
            index.remove(i);
        }
        for (int q = 0; q < 20_000; q++) {
            int x = random.nextInt(4401) - 2200;
            int z = random.nextInt(4401) - 2200;
            Set<Integer> expected = new HashSet<>();
            for (int i = 0; i < areas.size(); i++) {
                if (i < 100 && i % 3 == 0) {
                    continue;
                }
                long dx = (long) x - areas.get(i)[0];
                long dz = (long) z - areas.get(i)[1];
                long r = areas.get(i)[2];
                if (dx * dx + dz * dz <= r * r) {
                    expected.add(i);
                }
            }
            List<Integer> found = index.containing(x, z);
            assertEquals(expected, new HashSet<>(found), "Position " + x + ", " + z);
            assertTrue(found.size() == expected.size(), "No area two times");
        }
    }
}
