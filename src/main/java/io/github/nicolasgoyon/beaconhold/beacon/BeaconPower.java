package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.mixin.BeaconBlockEntityAccessor;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * The power state of a beacon. A beacon with a pyramid, a free beam and fuel is on. Each other beacon is off: it has no
 * beam, no vanilla powers, no function and no dome. A beacon is off until its first update.
 */
public final class BeaconPower {
    private BeaconPower() {
    }

    /**
     * Tells if a beacon is off. The client uses this value to hide the beam.
     *
     * @param beacon the beacon
     * @return true if the beacon was off after its last update, or if it has no update yet
     */
    public static boolean isOff(BeaconBlockEntity beacon) {
        return beacon.getData(ModRegistries.BEACON_OFF);
    }

    /**
     * Tells if a beacon on the server is off only because it has no consumable.
     *
     * @param beacon the beacon
     * @return true if the beacon is off, but has a pyramid and a free beam
     */
    public static boolean isOffWithNoConsumable(BeaconBlockEntity beacon) {
        boolean canOperate = ((BeaconBlockEntityAccessor) beacon).getBeaconholdLevels() > 0
                && !beacon.getBeamSections().isEmpty();
        return PowerRules.isOffWithNoConsumable(canOperate, !isOff(beacon));
    }

    /**
     * Saves the power state of a beacon after an update. The clients get the change.
     *
     * @param beacon the beacon
     * @param on true if the beacon has a pyramid, a free beam and fuel
     */
    public static void update(BeaconBlockEntity beacon, boolean on) {
        if (isOff(beacon) != !on || !beacon.hasData(ModRegistries.BEACON_OFF)) {
            beacon.setData(ModRegistries.BEACON_OFF, !on);
            beacon.setChanged();
            beacon.syncData(ModRegistries.BEACON_OFF);
        }
    }
}
