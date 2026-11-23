package io.github.nicolasgoyon.beaconhold.beacon;

/** The rules of the power of a beacon. This class uses no class of the game. */
public final class PowerRules {
    /** The default bonus to the level of each vanilla power of a beacon. */
    public static final int DEFAULT_LEVEL_BONUS = 1;

    /** The maximum bonus that the options accept. */
    public static final int MAX_LEVEL_BONUS = 4;

    private PowerRules() {
    }

    /**
     * Tells if a beacon is off only because it has no consumable.
     *
     * @param canOperate true if the beacon has a pyramid and a free beam
     * @param on true if the beacon can operate and has fuel
     * @return true if the beacon can operate but has no fuel. A beacon that cannot operate is off for another cause.
     */
    public static boolean isOffWithNoConsumable(boolean canOperate, boolean on) {
        return canOperate && !on;
    }

    /**
     * Tells if a beacon is on after an update.
     *
     * @param consumableRequired the value of the server option {@code cost.consumableRequired}
     * @param canOperate true if the beacon has a pyramid and a free beam
     * @param hasFuel true if the beacon has fuel after the update
     * @return true if the beacon can operate, and has fuel or needs no consumable
     */
    public static boolean isOn(boolean consumableRequired, boolean canOperate, boolean hasFuel) {
        return canOperate && (hasFuel || !consumableRequired);
    }

    /**
     * Gives the amplifier of a vanilla power with the bonus.
     *
     * @param vanillaAmplifier the amplifier that the vanilla beacon gives: 0 for level I, 1 for level II
     * @param bonus the bonus of the server option, from 0 to {@link #MAX_LEVEL_BONUS}
     * @return the amplifier with the bonus. A bonus under 0 counts as 0.
     */
    public static int amplifier(int vanillaAmplifier, int bonus) {
        return vanillaAmplifier + Math.max(0, bonus);
    }
}
