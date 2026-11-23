package io.github.nicolasgoyon.beaconhold.beacon;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * A spatial index of circular areas on the X and Z plane. The index puts each area in the cells of a grid that its
 * square touches. A query reads only the areas of one cell, so its cost does not grow with the number of areas far from
 * the position.
 *
 * <p>This class uses no class of the game. It is not thread-safe.
 *
 * @param <T> the type of the value of an area
 */
public final class AreaIndex<T> {
    /** The size of a cell is {@code 1 << CELL_SHIFT} blocks: 64 blocks. */
    public static final int CELL_SHIFT = 6;

    private final Long2ObjectMap<Entry<T>> entries = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<List<Entry<T>>> cells = new Long2ObjectOpenHashMap<>();

    /**
     * Adds an area, or replaces the area with the same key.
     *
     * @param key the key of the area, for example a packed block position
     * @param x the X coordinate of the center
     * @param z the Z coordinate of the center
     * @param radius the radius in blocks, 0 or more
     * @param value the value of the area
     */
    public void put(long key, int x, int z, int radius, T value) {
        remove(key);
        Entry<T> entry = new Entry<>(key, x, z, Math.max(0, radius), value);
        entries.put(key, entry);
        forEachCell(entry, cell -> cells.computeIfAbsent(cell, k -> new ArrayList<>(2)).add(entry));
    }

    /**
     * Removes an area.
     *
     * @param key the key of the area
     */
    public void remove(long key) {
        Entry<T> entry = entries.remove(key);
        if (entry == null) {
            return;
        }
        forEachCell(entry, cell -> {
            List<Entry<T>> list = cells.get(cell);
            if (list != null) {
                list.remove(entry);
                if (list.isEmpty()) {
                    cells.remove(cell);
                }
            }
        });
    }

    /** Removes all the areas. */
    public void clear() {
        entries.clear();
        cells.clear();
    }

    /**
     * Gives the number of areas.
     *
     * @return the number of areas in the index
     */
    public int size() {
        return entries.size();
    }

    /**
     * Gives the values of the areas that contain a column of blocks.
     *
     * @param x the X coordinate of the column
     * @param z the Z coordinate of the column
     * @return the values of the areas whose horizontal distance from the center to the column is equal to or less than
     *     the radius. The list is empty when no area contains the column.
     */
    public List<T> containing(int x, int z) {
        List<Entry<T>> list = cells.get(cellKey(x >> CELL_SHIFT, z >> CELL_SHIFT));
        if (list == null) {
            return List.of();
        }
        List<T> result = new ArrayList<>(1);
        for (Entry<T> entry : list) {
            if (entry.contains(x, z)) {
                result.add(entry.value());
            }
        }
        return result;
    }

    private void forEachCell(Entry<T> entry, CellAction action) {
        int minX = (entry.x() - entry.radius()) >> CELL_SHIFT;
        int maxX = (entry.x() + entry.radius()) >> CELL_SHIFT;
        int minZ = (entry.z() - entry.radius()) >> CELL_SHIFT;
        int maxZ = (entry.z() + entry.radius()) >> CELL_SHIFT;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                action.apply(cellKey(cx, cz));
            }
        }
    }

    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    @FunctionalInterface
    private interface CellAction {
        void apply(long cell);
    }

    private record Entry<T>(long key, int x, int z, int radius, T value) {
        boolean contains(int px, int pz) {
            long dx = (long) px - x;
            long dz = (long) pz - z;
            long r = radius;
            return dx * dx + dz * dz <= r * r;
        }
    }
}
