package io.github.nicolasgoyon.beaconhold.client.dome;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The colors and the opacities of the energy dome. The client options give the colors as hexadecimal texts
 * ({@code #RRGGBB}) and the opacities in percent. This class uses no class of the game.
 */
public final class DomeColors {
    /** The default color of the veil and of the ring: a light cyan, near the color of the beacon beam. */
    public static final String DEFAULT_COLOR = "#9EE8FF";

    /** The default maximum opacity of the veil, in percent. */
    public static final int DEFAULT_VEIL_OPACITY = (int) Math.round(DomeGeometry.VEIL_ALPHA * 100);

    /** The default maximum opacity of the ring, in percent. */
    public static final int DEFAULT_RING_OPACITY = (int) Math.round(DomeGeometry.RING_ALPHA * 100);

    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private static final int DEFAULT_RGB = 0x9EE8FF;

    private DomeColors() {
    }

    /**
     * Tells if a value is a valid color for the options.
     *
     * @param value the value
     * @return true for a text in the format {@code #RRGGBB}
     */
    public static boolean isValidColor(Object value) {
        return value instanceof String text && HEX_COLOR.matcher(text).matches();
    }

    /**
     * Reads a color.
     *
     * @param text a color in the format {@code #RRGGBB}
     * @return the color as an RGB number. An invalid text gives the default color.
     */
    public static int rgb(String text) {
        if (!isValidColor(text)) {
            return DEFAULT_RGB;
        }
        return Integer.parseInt(text.substring(1).toUpperCase(Locale.ROOT), 16);
    }

    /**
     * Gives the red part of a color.
     *
     * @param rgb the color
     * @return a value from 0 to 1
     */
    public static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    /**
     * Gives the green part of a color.
     *
     * @param rgb the color
     * @return a value from 0 to 1
     */
    public static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    /**
     * Gives the blue part of a color.
     *
     * @param rgb the color
     * @return a value from 0 to 1
     */
    public static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    /**
     * Gives the opacity of a point of the dome with the option of the player.
     *
     * @param alpha the opacity from {@link DomeGeometry}, from 0 to {@code defaultMaximum}
     * @param defaultMaximum the maximum opacity of {@link DomeGeometry}
     * @param percent the maximum opacity of the option, from 0 to 100
     * @return the opacity from 0 to {@code percent / 100}
     */
    public static float opacity(double alpha, double defaultMaximum, int percent) {
        if (defaultMaximum <= 0.0) {
            return 0.0F;
        }
        double scale = Math.max(0, Math.min(100, percent)) / 100.0 / defaultMaximum;
        return (float) Math.max(0.0, Math.min(1.0, alpha * scale));
    }
}
