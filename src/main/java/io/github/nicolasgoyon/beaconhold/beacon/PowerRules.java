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
     * Gives the bonus to the level of the vanilla powers. The bonus is the exchange for the cost of the beacon.
     *
     * @param consumableRequired the value of the server option {@code cost.consumableRequired}
     * @param optionBonus the value of the server option {@code vanillaPowers.levelBonus}
     * @return the bonus of the option when the beacon needs the consumable, else 0
     */
    public static int levelBonus(boolean consumableRequired, int optionBonus) {
        return consumableRequired ? Math.max(0, optionBonus) : 0;
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

    /**
     * Tells if a sound of the vanilla code of a beacon plays. A beacon that is off is silent. The vanilla activation
     * sound never plays: the beacon plays it when its power state goes on (see {@link #powerSound}).
     *
     * @param activation true for the activation sound
     * @param off true if the beacon is off
     * @return true if the sound plays
     */
    public static boolean playsVanillaSound(boolean activation, boolean off) {
        return !activation && !off;
    }

    /**
     * Gives the sound of a change of the power state of a beacon. The beacon plays the activation sound when it goes on. When the pyramid or the beam is lost, the vanilla tick plays the
     * deactivation sound, so the beacon plays it only when it goes off with no consumable.
     *
     * @param wasOn true if the beacon was on before the update
     * @param on true if the beacon is on after the update
     * @param canOperate true if the beacon has a pyramid and a free beam after the update
     * @return the sound to play
     */
    public static PowerSound powerSound(boolean wasOn, boolean on, boolean canOperate) {
        if (!wasOn && on) {
            return PowerSound.ACTIVATE;
        }
        if (wasOn && !on && canOperate) {
            return PowerSound.DEACTIVATE;
        }
        return PowerSound.NONE;
    }

    /** The sound of a change of the power state of a beacon. */
    public enum PowerSound {
        /** No sound. */
        NONE,
        /** The vanilla activation sound of the beacon. */
        ACTIVATE,
        /** The vanilla deactivation sound of the beacon. */
        DEACTIVATE
    }
}
