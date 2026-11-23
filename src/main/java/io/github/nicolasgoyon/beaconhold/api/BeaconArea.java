package io.github.nicolasgoyon.beaconhold.api;

import net.minecraft.core.BlockPos;

/**
 * The area of a beacon: a vertical cylinder for the full height of the world.
 *
 * <p>The axis of the cylinder goes through the center of the beacon. A block is in the area when its horizontal
 * distance to the beacon is equal to or less than the radius. The height of the block has no effect.
 *
 * @param center the position of the beacon
 * @param radius the radius in blocks. A radius of 0 gives an area that contains only the column of the beacon.
 */
public record BeaconArea(BlockPos center, int radius) {
    /**
     * Makes an area.
     *
     * @throws IllegalArgumentException if the radius is less than 0
     */
    public BeaconArea {
        if (radius < 0) {
            throw new IllegalArgumentException("The radius must be 0 or more: " + radius);
        }
        center = center.immutable();
    }

    /**
     * Tells if a position is in the area.
     *
     * @param pos the position
     * @return true if the horizontal distance from the center to the position is equal to or less than the radius
     */
    public boolean contains(BlockPos pos) {
        return containsColumn(pos.getX(), pos.getZ());
    }

    /**
     * Tells if a column of blocks is in the area.
     *
     * @param x the X coordinate of the column
     * @param z the Z coordinate of the column
     * @return true if the horizontal distance from the center to the column is equal to or less than the radius
     */
    public boolean containsColumn(int x, int z) {
        long dx = (long) x - center.getX();
        long dz = (long) z - center.getZ();
        long r = radius;
        return dx * dx + dz * dz <= r * r;
    }
}
