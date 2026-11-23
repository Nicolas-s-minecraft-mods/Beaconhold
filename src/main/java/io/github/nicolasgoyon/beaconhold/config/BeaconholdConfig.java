package io.github.nicolasgoyon.beaconhold.config;

import io.github.nicolasgoyon.beaconhold.beacon.PowerRules;
import io.github.nicolasgoyon.beaconhold.client.dome.DomeColors;
import io.github.nicolasgoyon.beaconhold.cost.CostRules;
import io.github.nicolasgoyon.beaconhold.function.ChunkRules;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The options of the mod. The server options are in the file {@code serverconfig/beaconhold-server.toml} of the world.
 * The client options are in the file {@code config/beaconhold-client.toml}.
 */
public final class BeaconholdConfig {
    /** The specification of the server options. */
    public static final ModConfigSpec SERVER_SPEC;

    /** The server options. */
    public static final Server SERVER;

    /** The specification of the client options. */
    public static final ModConfigSpec CLIENT_SPEC;

    /** The client options. */
    public static final Client CLIENT;

    /** The maximum value of the limit of the server that the options accept. */
    public static final int MAX_LOADED_CHUNKS = 100_000;

    static {
        Pair<Server, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getLeft();
        SERVER_SPEC = pair.getRight();
        Pair<Client, ModConfigSpec> clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT = clientPair.getLeft();
        CLIENT_SPEC = clientPair.getRight();
    }

    private BeaconholdConfig() {
    }

    /** The server options. */
    public static final class Server {
        /** The radius of the area in blocks, for the pyramid levels 1 to 4. */
        private final ModConfigSpec.IntValue[] radii;

        /** True if the vanilla powers of a beacon use the area of the beacon. */
        private final ModConfigSpec.BooleanValue vanillaPowersUseArea;

        /** The bonus to the level of each vanilla power of a beacon. */
        private final ModConfigSpec.IntValue levelBonus;

        /** True if the function "no hostile spawns" also stops the monster spawners. */
        private final ModConfigSpec.BooleanValue blockSpawners;

        /** The maximum number of chunks that all the beacons of the server load together. */
        private final ModConfigSpec.IntValue maxLoadedChunks;

        /** The number of ticks for each consumable item of an active function. */
        private final ModConfigSpec.IntValue ticksPerItem;

        /** True if a beacon needs the consumable to be on. */
        private final ModConfigSpec.BooleanValue consumableRequired;

        Server(ModConfigSpec.Builder builder) {
            builder.comment("The area of a beacon: a vertical cylinder around the beacon. The functions and the vanilla "
                            + "powers of the beacon use this area.")
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
            vanillaPowersUseArea = builder.comment("True: the vanilla powers of a beacon (Speed, Haste and the others) "
                            + "use the area of the beacon. False: the vanilla powers use the vanilla range.")
                    .translation(key("vanillaPowersUseArea"))
                    .define("vanillaPowersUseArea", true);
            builder.pop();

            builder.comment("The vanilla powers of a beacon.")
                    .translation(key("vanillaPowers"))
                    .push("vanillaPowers");
            levelBonus = builder.comment("The bonus to the level of each vanilla power of an active beacon. "
                            + "With 1, Speed I becomes Speed II. With cost.consumableRequired off, the powers get no bonus.")
                    .translation(key("levelBonus"))
                    .defineInRange("levelBonus", PowerRules.DEFAULT_LEVEL_BONUS, 0, PowerRules.MAX_LEVEL_BONUS);
            builder.pop();

            builder.comment("The function \"no hostile spawns\".")
                    .translation(key("noHostileSpawns"))
                    .push("noHostileSpawns");
            blockSpawners = builder.comment(
                            "True: the function also stops the spawns of the monster spawners and of the trial spawners in its area.")
                    .translation(key("blockSpawners"))
                    .define("blockSpawners", false);
            builder.pop();

            builder.comment("The function \"loaded chunks\".")
                    .translation(key("loadedChunks"))
                    .push("loadedChunks");
            maxLoadedChunks = builder.comment("The maximum number of chunks that all the beacons of the server load "
                            + "together. A beacon loads the chunks that its area touches. The command "
                            + "/beaconhold chunks limit shows and sets this value.")
                    .translation(key("maxLoadedChunks"))
                    .defineInRange("maxLoadedChunks", ChunkRules.DEFAULT_MAX_LOADED_CHUNKS, 0, MAX_LOADED_CHUNKS);
            builder.pop();

            builder.comment("The cost of the beacon functions.").translation(key("cost")).push("cost");
            consumableRequired = builder.comment("True: a beacon needs the consumable to be on. False: the vanilla "
                            + "rule. A beacon with a pyramid and a free beam is on, and it keeps its stock.")
                    .translation(key("consumableRequired"))
                    .define("consumableRequired", true);
            ticksPerItem = builder.comment("The number of ticks for each consumable item of an active function. "
                            + "With 3750 ticks, one stack of 64 items lasts 10 game days.")
                    .translation(key("ticksPerItem"))
                    .defineInRange(
                            "ticksPerItem",
                            CostRules.DEFAULT_TICKS_PER_ITEM,
                            CostRules.MIN_TICKS_PER_ITEM,
                            CostRules.MAX_TICKS_PER_ITEM);
            builder.pop();
        }

        /**
         * Tells if a beacon needs the consumable to be on.
         *
         * @return the value of the option {@code cost.consumableRequired}
         */
        public boolean consumableRequired() {
            return consumableRequired.getAsBoolean();
        }

        /**
         * Sets the option {@code cost.consumableRequired}. The GameTests use this method.
         *
         * @param value the new value
         */
        public void setConsumableRequired(boolean value) {
            consumableRequired.set(value);
        }

        /**
         * Gives the number of ticks for each consumable item of an active function.
         *
         * @return the value of the option {@code cost.ticksPerItem}
         */
        public int ticksPerItem() {
            return ticksPerItem.getAsInt();
        }

        /**
         * Sets the option {@code cost.ticksPerItem}. The GameTests use this method.
         *
         * @param value the new value
         */
        public void setTicksPerItem(int value) {
            ticksPerItem.set(value);
        }

        /**
         * Gives the maximum number of chunks that all the beacons of the server load together.
         *
         * @return the value of the option {@code loadedChunks.maxLoadedChunks}
         */
        public int maxLoadedChunks() {
            return maxLoadedChunks.getAsInt();
        }

        /**
         * Sets the option {@code loadedChunks.maxLoadedChunks}. The command and the GameTests use this method. The
         * method does not save the file.
         *
         * @param value the new value
         */
        public void setMaxLoadedChunks(int value) {
            maxLoadedChunks.set(value);
        }

        /**
         * Tells if the vanilla powers of a beacon use the area of the beacon.
         *
         * @return the value of the option {@code area.vanillaPowersUseArea}
         */
        public boolean vanillaPowersUseArea() {
            return vanillaPowersUseArea.getAsBoolean();
        }

        /**
         * Sets the option {@code area.vanillaPowersUseArea}. The GameTests use this method.
         *
         * @param value the new value
         */
        public void setVanillaPowersUseArea(boolean value) {
            vanillaPowersUseArea.set(value);
        }

        /**
         * Gives the bonus to the level of each vanilla power of a beacon.
         *
         * @return the value of the option {@code vanillaPowers.levelBonus}
         */
        public int levelBonus() {
            return levelBonus.getAsInt();
        }

        /**
         * Sets the option {@code vanillaPowers.levelBonus}. The GameTests use this method.
         *
         * @param value the new value
         */
        public void setLevelBonus(int value) {
            levelBonus.set(value);
        }

        /**
         * Tells if the function "no hostile spawns" also stops the monster spawners.
         *
         * @return the value of the option {@code noHostileSpawns.blockSpawners}
         */
        public boolean blockSpawners() {
            return blockSpawners.getAsBoolean();
        }

        /**
         * Sets the option {@code noHostileSpawns.blockSpawners}. The GameTests use this method.
         *
         * @param value the new value
         */
        public void setBlockSpawners(boolean value) {
            blockSpawners.set(value);
        }

        /**
         * Sets the radius of the area for a pyramid level. The GameTests use this method. The method does not save the
         * file.
         *
         * @param pyramidLevel the pyramid level, from 1 to 4
         * @param value the new radius in blocks
         */
        public void setRadius(int pyramidLevel, int value) {
            radii[pyramidLevel - 1].set(value);
        }

        /**
         * Gives the radius of the area for a pyramid level.
         *
         * @param pyramidLevel the pyramid level, from 0 to 4
         * @return the radius in blocks, or 0 for the pyramid level 0
         */
        public int radius(int pyramidLevel) {
            return AreaRadii.radius(pyramidLevel, radii());
        }

        /**
         * Gives the radii of the area.
         *
         * @return a new array with the radii in blocks, for the pyramid levels 1 to 4
         */
        public int[] radii() {
            int[] values = new int[radii.length];
            for (int i = 0; i < radii.length; i++) {
                values[i] = radii[i].getAsInt();
            }
            return values;
        }
    }

    /** The client options. */
    public static final class Client {
        /** True if the client shows the energy domes of the active beacons. */
        private final ModConfigSpec.BooleanValue domeEnabled;

        /** The color of the veil, in the format #RRGGBB. */
        private final ModConfigSpec.ConfigValue<String> veilColor;

        /** The maximum opacity of the veil, in percent. */
        private final ModConfigSpec.IntValue veilOpacity;

        /** The color of the ring, in the format #RRGGBB. */
        private final ModConfigSpec.ConfigValue<String> ringColor;

        /** The maximum opacity of the ring, in percent. */
        private final ModConfigSpec.IntValue ringOpacity;

        /** True if the client warns the player when the stock of a beacon is low. */
        private final ModConfigSpec.BooleanValue lowStockWarning;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("The energy dome: the visual of the area of an active beacon.")
                    .translation(key("dome"))
                    .push("dome");
            domeEnabled = builder.comment("True: the client shows the energy domes. False: the client shows no dome.")
                    .translation(key("domeEnabled"))
                    .define("enabled", true);
            veilColor = builder.comment("The color of the veil (the edge of the area from far), in the format #RRGGBB.")
                    .translation(key("veilColor"))
                    .define("veilColor", DomeColors.DEFAULT_COLOR, DomeColors::isValidColor);
            veilOpacity = builder.comment("The maximum opacity of the veil, in percent. 0: no veil.")
                    .translation(key("veilOpacity"))
                    .defineInRange("veilOpacity", DomeColors.DEFAULT_VEIL_OPACITY, 0, 100);
            ringColor = builder.comment("The color of the ring of light near the edge, in the format #RRGGBB.")
                    .translation(key("ringColor"))
                    .define("ringColor", DomeColors.DEFAULT_COLOR, DomeColors::isValidColor);
            ringOpacity = builder.comment("The maximum opacity of the ring, in percent. 0: no ring.")
                    .translation(key("ringOpacity"))
                    .defineInRange("ringOpacity", DomeColors.DEFAULT_RING_OPACITY, 0, 100);
            builder.pop();

            builder.comment("The warnings of the beacons.").translation(key("warnings")).push("warnings");
            lowStockWarning = builder.comment("True: a player in the area of a beacon gets a message and a sound when "
                            + "the stock of the beacon lasts less than one game day.")
                    .translation(key("lowStockWarning"))
                    .define("lowStock", true);
            builder.pop();
        }

        /**
         * Gives the color of the veil.
         *
         * @return the value of the option {@code dome.veilColor}, in the format #RRGGBB
         */
        public String veilColor() {
            return veilColor.get();
        }

        /**
         * Gives the maximum opacity of the veil.
         *
         * @return the value of the option {@code dome.veilOpacity}, in percent
         */
        public int veilOpacity() {
            return veilOpacity.getAsInt();
        }

        /**
         * Gives the color of the ring.
         *
         * @return the value of the option {@code dome.ringColor}, in the format #RRGGBB
         */
        public String ringColor() {
            return ringColor.get();
        }

        /**
         * Gives the maximum opacity of the ring.
         *
         * @return the value of the option {@code dome.ringOpacity}, in percent
         */
        public int ringOpacity() {
            return ringOpacity.getAsInt();
        }

        /**
         * Tells if the client warns the player when the stock of a beacon is low.
         *
         * @return the value of the option {@code warnings.lowStock}
         */
        public boolean lowStockWarning() {
            return lowStockWarning.getAsBoolean();
        }

        /**
         * Tells if the client shows the energy domes.
         *
         * @return the value of the option {@code dome.enabled}
         */
        public boolean domeEnabled() {
            return domeEnabled.getAsBoolean();
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
