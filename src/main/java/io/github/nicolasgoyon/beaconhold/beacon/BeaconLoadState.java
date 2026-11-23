package io.github.nicolasgoyon.beaconhold.beacon;

/** The state of a beacon after a load (see {@link BeaconLoadRules}). Each beacon block entity has this state. */
public interface BeaconLoadState {
    /**
     * Gives the state of the beacon.
     *
     * @return {@link BeaconLoadRules#LOADED}, {@link BeaconLoadRules#SCANNED} or {@link BeaconLoadRules#READY}
     */
    int beaconhold$loadState();
}
