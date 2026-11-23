package io.github.nicolasgoyon.beaconhold.cost;

import java.util.Locale;

/**
 * The rules of the cost of a function. This class uses no class of the game.
 *
 * <p>An active function uses one consumable item for each period of {@code ticksPerItem} ticks. With the default
 * period, one stack of 64 items lasts 10 game days (240,000 ticks).
 */
public final class CostRules {
    /** The number of ticks in one game day. */
    public static final int TICKS_PER_DAY = 24_000;

    /** The number of ticks in one second. */
    public static final int TICKS_PER_SECOND = 20;

    /** The number of items in one stack of the consumable. */
    public static final int STACK_SIZE = 64;

    /** The default number of ticks for each item: one stack lasts 10 game days. */
    public static final int DEFAULT_TICKS_PER_ITEM = 3_750;

    /** The minimum number of ticks for each item that the options accept: the interval of the beacon updates. */
    public static final int MIN_TICKS_PER_ITEM = 80;

    /** The maximum number of ticks for each item that the options accept: one game day. */
    public static final int MAX_TICKS_PER_ITEM = TICKS_PER_DAY;

    private CostRules() {
    }

    /**
     * Gives the full minutes of a time.
     *
     * @param ticks the time in ticks
     * @return the number of full minutes
     */
    public static int minutes(int ticks) {
        return Math.max(0, ticks) / TICKS_PER_SECOND / 60;
    }

    /**
     * Gives the seconds of a time, after the full minutes.
     *
     * @param ticks the time in ticks
     * @return the seconds, from 0 to 59
     */
    public static int seconds(int ticks) {
        return Math.max(0, ticks) / TICKS_PER_SECOND % 60;
    }

    /**
     * Gives the time that a number of items lasts.
     *
     * @param items the number of items
     * @param ticksPerItem the number of ticks for each item
     * @return the time in ticks
     */
    public static long duration(int items, int ticksPerItem) {
        return (long) Math.max(0, items) * Math.max(0, ticksPerItem);
    }

    /**
     * Gives the number of game days that a stack lasts, as a text.
     *
     * @param ticksPerItem the number of ticks for each item
     * @return the days with one decimal, or with no decimal for a full number. Example: "10" or "2.5".
     */
    public static String stackDays(int ticksPerItem) {
        return days(duration(STACK_SIZE, ticksPerItem));
    }

    /**
     * Gives a time in game days, as a text.
     *
     * @param ticks the time in ticks
     * @return the days with one decimal, or with no decimal for a full number
     */
    public static String days(long ticks) {
        long tenths = Math.round(Math.max(0L, ticks) * 10.0 / TICKS_PER_DAY);
        if (tenths % 10 == 0) {
            return Long.toString(tenths / 10);
        }
        return String.format(Locale.ROOT, "%d.%d", tenths / 10, tenths % 10);
    }

    /**
     * Gives the signal of a comparator next to a beacon, from the level of the stock. The formula is the formula of a
     * container with one slot.
     *
     * @param count the number of items in the stock
     * @param maxStackSize the maximum stack size of the item
     * @return 0 for an empty stock, 1 to 14 for a partial stock, 15 for a full stack
     */
    public static int comparatorSignal(int count, int maxStackSize) {
        if (count <= 0 || maxStackSize <= 0) {
            return 0;
        }
        int clamped = Math.min(count, maxStackSize);
        return Math.min(15, clamped * 14 / maxStackSize + 1);
    }
}
