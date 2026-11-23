package io.github.nicolasgoyon.beaconhold.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;

/**
 * A beacon with an active function.
 *
 * <p>A function is active when the beacon has the function and a pyramid level of 1 or more, no block stops the beam,
 * and the beacon has fuel or a stock of the consumable.
 *
 * @param pos the position of the beacon
 * @param pyramidLevel the number of complete layers of the pyramid, from 1 to 4
 * @param function the function of the beacon
 * @param area the area of the function
 */
public record ActiveBeacon(BlockPos pos, int pyramidLevel, ResourceKey<BeaconFunction> function, BeaconArea area) {
    /** Makes an active beacon. */
    public ActiveBeacon {
        pos = pos.immutable();
    }
}
