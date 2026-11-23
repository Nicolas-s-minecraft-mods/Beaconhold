package io.github.nicolasgoyon.beaconhold.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** The options of the mod. The server options are in the file {@code serverconfig/beaconhold-server.toml}. */
public final class BeaconholdConfig {
    /** The specification of the server options. */
    public static final ModConfigSpec SERVER_SPEC;

    /** The server options. */
    public static final Server SERVER;

    static {
        Pair<Server, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
    }

    private BeaconholdConfig() {
    }

    /** The server options. */
    public static final class Server {
        /** The radius of the area in blocks, for the pyramid levels 1 to 4. */
        private final ModConfigSpec.IntValue[] radii;

        Server(ModConfigSpec.Builder builder) {
            builder.comment("The area of the beacon functions: a vertical cylinder around the beacon.")
                    .translation(key("area"))
                    .push("area");
            int[] defaults = AreaRadii.DEFAULT_RADII;
            radii = new ModConfigSpec.IntValue[defaults.length];
            for (int i = 0; i < defaults.length; i++) {
                String name = "radiusLevel" + (i + 1);
                radii[i] = builder.comment("The radius of the area, in blocks, for the pyramid level " + (i + 1) + ".")
                        .translation(key(name))
                        .defineInRange(name, defaults[i], 1, AreaRadii.MAX_RADIUS);
            }
            builder.pop();
        }

        /**
         * Gives the radius of the area for a pyramid level.
         *
         * @param pyramidLevel the pyramid level, from 0 to 4
         * @return the radius in blocks, or 0 for the pyramid level 0
         */
        public int radius(int pyramidLevel) {
            int[] values = new int[radii.length];
            for (int i = 0; i < radii.length; i++) {
                values[i] = radii[i].getAsInt();
            }
            return AreaRadii.radius(pyramidLevel, values);
        }
    }

    /**
     * Gives the translation key of an option.
     *
     * @param name the name of the option
     * @return the key {@code beaconhold.configuration.<name>}
     */
    static String key(String name) {
        return "beaconhold.configuration." + name;
    }
}
