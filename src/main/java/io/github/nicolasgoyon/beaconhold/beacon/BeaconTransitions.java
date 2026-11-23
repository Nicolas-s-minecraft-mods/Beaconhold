package io.github.nicolasgoyon.beaconhold.beacon;

import javax.annotation.Nullable;

/** The rule that gives the events of a change of the state of a beacon. */
public final class BeaconTransitions {
    private BeaconTransitions() {
    }

    /**
     * Gives the events of a change of the state of a beacon.
     *
     * <p>A change of the function or of the pyramid level of an active beacon gives a stop event for the old state,
     * then a start event for the new state.
     *
     * @param before the state before the change, or {@code null} if the beacon had no function
     * @param after the state after the change, or {@code null} if the beacon has no function
     * @return the events of the change
     */
    public static Transition of(@Nullable BeaconRecord before, @Nullable BeaconRecord after) {
        boolean same = before != null && after != null && before.sameActiveFunction(after);
        BeaconRecord stop = before != null && before.active() && !same ? before : null;
        BeaconRecord start = after != null && after.active() && !same ? after : null;
        return new Transition(stop, start);
    }

    /**
     * The events of a change of the state of a beacon.
     *
     * @param stop the state for the stop event, or {@code null} if the change stops no function
     * @param start the state for the start event, or {@code null} if the change starts no function
     */
    public record Transition(@Nullable BeaconRecord stop, @Nullable BeaconRecord start) {}
}
