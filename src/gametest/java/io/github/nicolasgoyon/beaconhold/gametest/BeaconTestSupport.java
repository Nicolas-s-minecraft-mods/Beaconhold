package io.github.nicolasgoyon.beaconhold.gametest;

import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/** Builds beacons in the GameTests. The template {@code beaconhold:beacon} has a size of 5 × 6 × 5 blocks. */
final class BeaconTestSupport {
    /** The relative position of the beacon in the template. */
    static final BlockPos BEACON = new BlockPos(2, 2, 2);

    private BeaconTestSupport() {
    }

    /**
     * Builds a pyramid of iron blocks and a beacon on it.
     *
     * @param helper the helper of the test
     * @param pyramidLevel the pyramid level, from 0 to 2
     * @return the absolute position of the beacon
     */
    static BlockPos buildBeacon(GameTestHelper helper, int pyramidLevel) {
        for (int layer = 1; layer <= pyramidLevel; layer++) {
            addLayer(helper, layer);
        }
        helper.setBlock(BEACON, Blocks.BEACON);
        return helper.absolutePos(BEACON);
    }

    /**
     * Adds one layer of iron blocks under the beacon.
     *
     * @param helper the helper of the test
     * @param layer the layer, from 1 (under the beacon) to 2
     */
    static void addLayer(GameTestHelper helper, int layer) {
        for (int x = -layer; x <= layer; x++) {
            for (int z = -layer; z <= layer; z++) {
                helper.setBlock(BEACON.offset(x, -layer, z), Blocks.IRON_BLOCK);
            }
        }
    }

    /**
     * Removes the function and the beacon, so that the beacon has no effect on the next tests.
     *
     * @param helper the helper of the test
     */
    static void removeBeacon(GameTestHelper helper) {
        BeaconholdApi.setFunction(helper.getLevel(), helper.absolutePos(BEACON), null);
        helper.setBlock(BEACON, Blocks.AIR);
    }
}
