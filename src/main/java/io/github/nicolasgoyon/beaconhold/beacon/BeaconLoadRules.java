package io.github.nicolasgoyon.beaconhold.beacon;

/**
 * The rules that tell when a loaded beacon knows its pyramid level and its beam.
 *
 * <p>The game does not read the pyramid level and the beam from the save. A loaded beacon scans its beam again, then
 * calculates its pyramid level at the next periodic update. Until then, an active beacon keeps the state of its saved
 * record. A change of the function applies at once.
 */
public final class BeaconLoadRules {
    /** The beacon is loaded. It has not scanned its beam yet. */
    public static final int LOADED = 0;

    /** The beacon has scanned its beam. It calculates its pyramid level at the next periodic update. */
    public static final int SCANNED = 1;

    /** The beacon knows its beam and its pyramid level. */
    public static final int READY = 2;

    private BeaconLoadRules() {
    }

    /**
     * Gives the state of a beacon after a tick.
     *
     * @param state the state before the tick
     * @param periodic true if the tick is a periodic update. The game calculates the pyramid level in this tick, before
     *     the end of the scan of the beam.
     * @param scanCompleted true if the beacon completed the scan of its beam in this tick
     * @return the state after the tick
     */
    public static int afterTick(int state, boolean periodic, boolean scanCompleted) {
        int result = state;
        if (periodic && result == SCANNED) {
            result = READY;
        }
        if (scanCompleted && result == LOADED) {
            result = SCANNED;
        }
        return result;
    }

    /**
     * Tells if an update keeps the saved record of a beacon.
     *
     * @param state the state of the beacon
     * @param savedActive true if the saved record of the beacon is active with the current function of the beacon
     * @return true if the beacon is active in its record and does not know its pyramid level and its beam yet
     */
    public static boolean keepsSavedState(int state, boolean savedActive) {
        return savedActive && state != READY;
    }
}
