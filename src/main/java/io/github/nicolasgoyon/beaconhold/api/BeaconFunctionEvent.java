package io.github.nicolasgoyon.beaconhold.api;

import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * An event for a beacon that starts or stops a function, or for a change of the area of an active function.
 *
 * <p>The server posts these events on the NeoForge event bus ({@code NeoForge.EVENT_BUS}), on the server thread. A
 * change of the pyramid level of an active beacon gives a {@link Stop} event with the old area, then a {@link Start}
 * event with the new area. A change of the server options gives no {@link Start} event and no {@link Stop} event. It
 * gives an {@link AreaChange} event for each active beacon whose radius changes.
 *
 * <p>Register a listener for {@link Start}, {@link Stop} or {@link AreaChange}. The event bus refuses a listener for
 * this abstract class.
 */
public abstract class BeaconFunctionEvent extends Event {
    private final ServerLevel level;
    private final ActiveBeacon beacon;

    private BeaconFunctionEvent(ServerLevel level, ActiveBeacon beacon) {
        this.level = level;
        this.beacon = beacon;
    }

    /**
     * Gives the dimension of the beacon.
     *
     * @return the level of the beacon
     */
    public ServerLevel getLevel() {
        return level;
    }

    /**
     * Gives the beacon and its function.
     *
     * @return the beacon, with the area of the function
     */
    public ActiveBeacon getBeacon() {
        return beacon;
    }

    /** A beacon starts a function. The queries of {@link BeaconholdApi} include the beacon. */
    public static final class Start extends BeaconFunctionEvent {
        /**
         * Makes the event.
         *
         * @param level the dimension of the beacon
         * @param beacon the beacon and its function
         */
        public Start(ServerLevel level, ActiveBeacon beacon) {
            super(level, beacon);
        }
    }

    /**
     * A beacon stops a function. The queries of {@link BeaconholdApi} do not include the beacon.
     *
     * <p>The beacon stops its function when a block stops the beam, when the pyramid level is 0, when the beacon has no
     * consumable, when the beacon gets a different function or no function, and when a player or an explosion removes
     * the beacon.
     */
    public static final class Stop extends BeaconFunctionEvent {
        /**
         * Makes the event.
         *
         * @param level the dimension of the beacon
         * @param beacon the beacon and its function, as they were when the function was active
         */
        public Stop(ServerLevel level, ActiveBeacon beacon) {
            super(level, beacon);
        }
    }

    /**
     * A change of the server options changes the area of an active function. The function stays active. The queries
     * of {@link BeaconholdApi} use the new area.
     *
     * <p>The server posts this event after it applies the new options, while the server runs. A change of the options
     * file of a stopped server gives no event.
     */
    public static final class AreaChange extends BeaconFunctionEvent {
        private final BeaconArea oldArea;

        /**
         * Makes the event.
         *
         * @param level the dimension of the beacon
         * @param beacon the beacon and its function, with the new area
         * @param oldArea the area before the change of the options
         */
        public AreaChange(ServerLevel level, ActiveBeacon beacon, BeaconArea oldArea) {
            super(level, beacon);
            this.oldArea = Objects.requireNonNull(oldArea);
        }

        /**
         * Gives the area before the change of the options. {@link #getBeacon()} gives the new area.
         *
         * @return the old area
         */
        public BeaconArea getOldArea() {
            return oldArea;
        }
    }
}
