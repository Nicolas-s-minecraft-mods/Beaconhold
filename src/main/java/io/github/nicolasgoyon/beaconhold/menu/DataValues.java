package io.github.nicolasgoyon.beaconhold.menu;

/**
 * Splits a large value into two data slots of a menu. The game sends a data slot as a 16-bit signed number, so one
 * slot holds a value from 0 to 32767 only. Each part holds 15 bits. This class uses no class of the game.
 */
public final class DataValues {
    private static final int BITS = 15;
    private static final int MASK = (1 << BITS) - 1;

    private DataValues() {
    }

    /**
     * Gives the low part of a value.
     *
     * @param value a value from 0 to {@link Integer#MAX_VALUE} {@code >> 15}
     * @return the 15 low bits
     */
    public static int low(int value) {
        return value & MASK;
    }

    /**
     * Gives the high part of a value.
     *
     * @param value a value from 0 to {@link Integer#MAX_VALUE} {@code >> 15}
     * @return the bits above the 15 low bits
     */
    public static int high(int value) {
        return value >>> BITS;
    }

    /**
     * Joins the two parts of a value. The parts can come from 16-bit signed numbers.
     *
     * @param low the low part
     * @param high the high part
     * @return the value
     */
    public static int join(int low, int high) {
        return ((high & MASK) << BITS) | (low & MASK);
    }
}
