package io.github.nicolasgoyon.beaconhold.api;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconTracker;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * The entry point of the API of Beaconhold.
 *
 * <p>All the methods that take a {@link ServerLevel} operate on the server thread only. The queries use the saved list
 * of the beacons of the dimension. They do not load chunks. A beacon in a chunk that is not loaded keeps its last
 * state.
 */
public final class BeaconholdApi {
    /** The id of the mod. */
    public static final String MOD_ID = "beaconhold";

    /** The key of the registry of the beacon functions: {@code beaconhold:beacon_function}. */
    public static final ResourceKey<Registry<BeaconFunction>> FUNCTION_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "beacon_function"));

    private BeaconholdApi() {
    }

    /**
     * Gives the registry of the beacon functions.
     *
     * @return the registry, with the functions of all the mods
     */
    public static Registry<BeaconFunction> functions() {
        return ModRegistries.FUNCTIONS;
    }

    /**
     * Gives the active beacons with a function that cover a position.
     *
     * @param level the dimension
     * @param pos the position
     * @param function the function
     * @return the beacons with this active function whose area contains the position. The list is empty when no
     *     beacon covers the position.
     */
    public static List<ActiveBeacon> beaconsCovering(
            ServerLevel level, BlockPos pos, ResourceKey<BeaconFunction> function) {
        return BeaconTracker.beaconsCovering(level, pos, function);
    }

    /**
     * Tells if a position is in the area of an active beacon with a function.
     *
     * @param level the dimension
     * @param pos the position
     * @param function the function
     * @return true if at least one active beacon with this function covers the position
     */
    public static boolean isCovered(ServerLevel level, BlockPos pos, ResourceKey<BeaconFunction> function) {
        return BeaconTracker.isCovered(level, pos, function);
    }

    /**
     * Gives all the active beacons of a dimension.
     *
     * @param level the dimension
     * @return the beacons with an active function
     */
    public static List<ActiveBeacon> activeBeacons(ServerLevel level) {
        return BeaconTracker.activeBeacons(level);
    }

    /**
     * Gives the function of a beacon. The function can be active or not.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @return the function of the beacon, or an empty value if there is no beacon or the beacon has no function
     */
    public static Optional<ResourceKey<BeaconFunction>> getFunction(ServerLevel level, BlockPos pos) {
        return BeaconTracker.getFunction(level, pos);
    }

    /**
     * Sets the function of a beacon. The beacon must be in a loaded chunk.
     *
     * <p>The beacon updates its state at once. It posts the {@link BeaconFunctionEvent} events of the change.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @param function the new function, or {@code null} to remove the function
     * @return true if there is a beacon at the position, false if not
     * @throws IllegalArgumentException if the registry does not contain the function
     */
    public static boolean setFunction(ServerLevel level, BlockPos pos, @Nullable ResourceKey<BeaconFunction> function) {
        return BeaconTracker.setFunction(level, pos, function);
    }
}
