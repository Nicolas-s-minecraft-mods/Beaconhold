package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.api.BeaconArea;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The rule that gives the players that get the vanilla powers of a beacon (Speed, Haste and the others). The players in
 * the area of the beacon get the powers. The area is the same as for the functions of the beacon.
 */
public final class VanillaPowerArea {
    private VanillaPowerArea() {
    }

    /**
     * Gives the box that contains the area of a beacon, from the bottom to the top of the world.
     *
     * @param center the position of the beacon
     * @param radius the radius of the area in blocks
     * @param minY the lowest Y coordinate of the world
     * @param maxY the Y coordinate above the highest block of the world
     * @return the box. Each column of blocks of the area is in the box.
     */
    public static AABB searchBox(BlockPos center, int radius, int minY, int maxY) {
        return new AABB(
                center.getX() - radius,
                minY,
                center.getZ() - radius,
                center.getX() + radius + 1,
                maxY,
                center.getZ() + radius + 1);
    }

    /**
     * Gives the players in the area of a beacon.
     *
     * @param level the dimension of the beacon
     * @param center the position of the beacon
     * @param radius the radius of the area in blocks
     * @return the players that are not spectators, with their block position in the area
     */
    public static List<Player> playersInArea(Level level, BlockPos center, int radius) {
        BeaconArea area = new BeaconArea(center, radius);
        AABB box = searchBox(center, radius, level.getMinBuildHeight(), level.getMaxBuildHeight());
        return level.getEntitiesOfClass(Player.class, box, player -> !player.isSpectator()
                && area.contains(player.blockPosition()));
    }
}
