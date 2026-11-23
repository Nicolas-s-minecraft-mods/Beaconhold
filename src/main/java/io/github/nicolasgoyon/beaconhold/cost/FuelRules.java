package io.github.nicolasgoyon.beaconhold.cost;

/**
 * The rules of the consumption of the consumable. This class uses no class of the game.
 *
 * <p>A beacon has a stock (a number of items) and a fuel (a number of ticks). Each update first removes from the fuel
 * the ticks of operation since the last update. Then, when the fuel is at 0 or less and the function can operate, the
 * beacon takes one item from the stock and adds {@code ticksPerItem} ticks to the fuel. Thus one item lasts
 * {@code ticksPerItem} ticks, and a stack of 64 items lasts {@code 64 * ticksPerItem} ticks.
 */
public final class FuelRules {
    /** Below this number of ticks of stock and fuel, the beacon gives a sign to the players: one game day. */
    public static final long LOW_STOCK_TICKS = CostRules.TICKS_PER_DAY;

    /**
     * The maximum number of ticks of operation between two updates: the interval of the beacon updates. A beacon in a
     * chunk that is not loaded does not operate, so the time out of the loaded chunks does not count.
     */
    public static final int MAX_ELAPSED = 80;

    private FuelRules() {
    }

    /**
     * Gives the number of ticks of operation since the last update.
     *
     * @param now the game time of this update
     * @param last the game time of the last update
     * @param wasActive true if the function was active after the last update
     * @return 0 if the function was not active, else the ticks since the last update, at most {@link #MAX_ELAPSED}
     */
    public static int elapsed(long now, long last, boolean wasActive) {
        if (!wasActive) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(now - last, MAX_ELAPSED));
    }

    /**
     * Gives the state of the stock after one update.
     *
     * @param stock the number of items in the stock
     * @param fuel the number of ticks of fuel
     * @param ticksPerItem the number of ticks for each item
     * @param elapsed the number of ticks of operation since the last update
     * @param canOperate true if the beacon has a function, a pyramid and a free beam
     * @return the new state. The function is active only if the beacon can operate and has fuel. A fuel under 0 is a
     *     debt that the next item pays.
     */
    public static State update(int stock, int fuel, int ticksPerItem, int elapsed, boolean canOperate) {
        int left = fuel - Math.max(0, elapsed);
        if (!canOperate) {
            return new State(stock, left, false);
        }
        int perItem = Math.max(1, ticksPerItem);
        while (left <= 0 && stock > 0) {
            stock--;
            left += perItem;
        }
        return new State(stock, left, left > 0);
    }

    /**
     * Gives the time of operation that a beacon has in its stock and in its fuel.
     *
     * @param stock the number of items in the stock
     * @param fuel the number of ticks of fuel
     * @param ticksPerItem the number of ticks for each item
     * @return the time in ticks
     */
    public static long remainingTicks(int stock, int fuel, int ticksPerItem) {
        return CostRules.duration(stock, ticksPerItem) + Math.max(0, fuel);
    }

    /**
     * Tells if the stock is low.
     *
     * @param remainingTicks the time of operation in the stock and in the fuel
     * @return true if the time is less than {@link #LOW_STOCK_TICKS}
     */
    public static boolean isLow(long remainingTicks) {
        return remainingTicks < LOW_STOCK_TICKS;
    }

    /**
     * The state of the stock of a beacon.
     *
     * @param stock the number of items in the stock
     * @param fuel the number of ticks of fuel
     * @param active true if the function is active
     */
    public record State(int stock, int fuel, boolean active) {}
}
