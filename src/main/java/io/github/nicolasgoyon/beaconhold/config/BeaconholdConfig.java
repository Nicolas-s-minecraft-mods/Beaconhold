package io.github.nicolasgoyon.beaconhold.config;

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

    /** The maximum number of chunks for one beacon that the options accept. */
    private static final int MAX_CHUNKS_PER_BEACON = 81;

    /** The maximum value of the limit of the server that the options accept. */
    private static final int MAX_LOADED_CHUNKS = 100_000;

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

        /** True if the function "no hostile spawns" also stops the monster spawners. */
        private final ModConfigSpec.BooleanValue blockSpawners;

        /** The number of chunks that the function "loaded chunks" loads, for the pyramid levels 1 to 4. */
        private final ModConfigSpec.IntValue[] chunkCounts;

        /** The maximum number of chunks that all the beacons of the server load together. */
        private final ModConfigSpec.IntValue maxLoadedChunks;

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

            builder.comment("The function \"no hostile spawns\".")
                    .translation(key("noHostileSpawns"))
                    .push("noHostileSpawns");
            blockSpawners = builder.comment(
                            "True: the function also stops the spawns of the monster spawners in its area.")
                    .translation(key("blockSpawners"))
                    .define("blockSpawners", false);
            builder.pop();

            builder.comment("The function \"loaded chunks\".")
                    .translation(key("loadedChunks"))
                    .push("loadedChunks");
            int[] chunkDefaults = ChunkRules.defaultChunks();
            chunkCounts = new ModConfigSpec.IntValue[chunkDefaults.length];
            for (int i = 0; i < chunkDefaults.length; i++) {
                String name = "chunksLevel" + (i + 1);
                chunkCounts[i] = builder.comment("The number of chunks that a beacon loads for the pyramid level "
                                + (i + 1) + ". The beacon loads its own chunk first, then the nearest chunks.")
                        .translation(key(name))
                        .defineInRange(name, chunkDefaults[i], 0, MAX_CHUNKS_PER_BEACON);
            }
            maxLoadedChunks = builder.comment(
                            "The maximum number of chunks that all the beacons of the server load together.")
                    .translation(key("maxLoadedChunks"))
                    .defineInRange("maxLoadedChunks", ChunkRules.DEFAULT_MAX_LOADED_CHUNKS, 0, MAX_LOADED_CHUNKS);
            builder.pop();
        }

        /**
         * Gives the number of chunks that the function "loaded chunks" loads for a pyramid level.
         *
         * @param pyramidLevel the pyramid level, from 0 to 4
         * @return the number of chunks, or 0 for the pyramid level 0
         */
        public int chunkCount(int pyramidLevel) {
            int[] values = new int[chunkCounts.length];
            for (int i = 0; i < chunkCounts.length; i++) {
                values[i] = chunkCounts[i].getAsInt();
            }
            return ChunkRules.chunkCount(pyramidLevel, values);
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
         * Sets the option {@code loadedChunks.maxLoadedChunks}. The GameTests use this method.
         *
         * @param value the new value
         */
        public void setMaxLoadedChunks(int value) {
            maxLoadedChunks.set(value);
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

    /** The client options. */
    public static final class Client {
        /** True if the client shows the energy domes of the active beacons. */
        private final ModConfigSpec.BooleanValue domeEnabled;

        Client(ModConfigSpec.Builder builder) {
            builder.comment("The energy dome: the visual of the area of an active beacon.")
                    .translation(key("dome"))
                    .push("dome");
            domeEnabled = builder.comment("True: the client shows the energy domes. False: the client shows no dome.")
                    .translation(key("domeEnabled"))
                    .define("enabled", true);
            builder.pop();
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
