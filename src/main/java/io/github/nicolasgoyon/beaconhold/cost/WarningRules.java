package io.github.nicolasgoyon.beaconhold.cost;

import javax.annotation.Nullable;

/** The rules of the warning for a low stock of the consumable. This class uses no class of the game. */
public final class WarningRules {
    /** The minimum number of ticks between two warnings of the same beacon on a client: 5 minutes. */
    public static final long COOLDOWN_TICKS = 5L * 60 * CostRules.TICKS_PER_SECOND;

    private WarningRules() {
    }

    /**
     * Tells if the stock of an active beacon is low enough for a warning.
     *
     * @param remainingTicks the time of operation that the stock and the fuel give, in ticks
     * @return true if the time is less than one game day
     */
    public static boolean isLow(long remainingTicks) {
        return FuelRules.isLow(remainingTicks);
    }

    /**
     * Tells if a client shows a new warning for a beacon.
     *
     * @param lastWarning the client tick of the last warning for the beacon, or {@code null} if there was no warning
     * @param now the current client tick
     * @return true if there was no warning, or if the last warning is {@link #COOLDOWN_TICKS} ticks old or more
     */
    public static boolean shouldWarn(@Nullable Long lastWarning, long now) {
        return lastWarning == null || now - lastWarning >= COOLDOWN_TICKS || now < lastWarning;
    }
}
