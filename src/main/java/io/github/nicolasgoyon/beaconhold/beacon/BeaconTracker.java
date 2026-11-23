package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.api.ActiveBeacon;
import io.github.nicolasgoyon.beaconhold.api.BeaconArea;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.api.BeaconFunctionEvent;
import io.github.nicolasgoyon.beaconhold.api.BeaconholdApi;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.cost.BeaconStock;
import io.github.nicolasgoyon.beaconhold.mixin.BeaconBlockEntityAccessor;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Keeps the saved list of the beacons with a function, and posts the events of the changes.
 *
 * <p>Each beacon updates its record each {@link #UPDATE_INTERVAL} ticks, in the same tick as its vanilla powers. At each
 * update, an operating beacon uses its stock of the consumable. A loaded beacon keeps its saved active state until it
 * knows its pyramid level and its beam (see {@link BeaconLoadRules}). The dimension also removes the records of the loaded
 * positions that have no beacon.
 */
public final class BeaconTracker {
    /** The number of ticks between two updates of a beacon. The game uses the same interval for the beacon powers. */
    public static final long UPDATE_INTERVAL = 80L;

    /** The tick in the interval when the dimension removes the records with no beacon. */
    private static final long CLEANUP_TICK = UPDATE_INTERVAL / 2;

    private BeaconTracker() {
    }

    /** Adds the listeners of the tracker to the NeoForge event bus. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, BeaconTracker::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % UPDATE_INTERVAL == CLEANUP_TICK) {
            removeMissingBeacons(level);
        }
    }

    /**
     * Updates the record of a beacon from its block entity, out of the interval of the updates.
     *
     * @param level the dimension of the beacon
     * @param beacon the block entity of the beacon
     */
    public static void update(ServerLevel level, BeaconBlockEntity beacon) {
        update(level, beacon, false);
    }

    /**
     * Updates the record of a beacon from its block entity, and uses its stock of the consumable.
     *
     * @param level the dimension of the beacon
     * @param beacon the block entity of the beacon
     * @param periodic true for the update at the interval of the vanilla powers
     */
    public static void update(ServerLevel level, BeaconBlockEntity beacon, boolean periodic) {
        BlockPos pos = beacon.getBlockPos();
        BeaconSavedData data = BeaconSavedData.get(level);
        BeaconRecord before = data.get(pos);
        Optional<ResourceKey<BeaconFunction>> current = readFunction(beacon);
        boolean savedActive = before != null
                && before.active()
                && current.filter(function -> function.location().equals(before.function())).isPresent();
        if (BeaconLoadRules.keepsSavedState(((BeaconLoadState) beacon).beaconhold$loadState(), savedActive)) {
            return;
        }
        BeaconRecord after = current
                .map(function -> {
                    int pyramidLevel = ((BeaconBlockEntityAccessor) beacon).getBeaconholdLevels();
                    boolean canOperate = pyramidLevel > 0 && !beacon.getBeamSections().isEmpty();
                    boolean wasActive = before != null && before.active();
                    boolean active = BeaconStock.operate(level, beacon, wasActive, canOperate, periodic);
                    return new BeaconRecord(pos, pyramidLevel, active, function.location());
                })
                .orElse(null);
        apply(level, data, pos, before, after);
    }

    /**
     * Removes the records of the loaded positions that have no beacon. The records in the chunks that are not loaded
     * stay.
     *
     * @param level the dimension
     */
    public static void removeMissingBeacons(ServerLevel level) {
        BeaconSavedData data = BeaconSavedData.get(level);
        for (BeaconRecord beacon : data.copy()) {
            BlockPos pos = beacon.pos();
            if (level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof BeaconBlockEntity)) {
                apply(level, data, pos, beacon, null);
            }
        }
    }

    private static void apply(
            ServerLevel level,
            BeaconSavedData data,
            BlockPos pos,
            @Nullable BeaconRecord before,
            @Nullable BeaconRecord after) {
        if (Objects.equals(before, after)) {
            return;
        }
        data.set(pos, after);
        BeaconTransitions.Transition transition = BeaconTransitions.of(before, after);
        if (transition.stop() != null) {
            NeoForge.EVENT_BUS.post(new BeaconFunctionEvent.Stop(level, toActiveBeacon(transition.stop())));
        }
        if (transition.start() != null) {
            NeoForge.EVENT_BUS.post(new BeaconFunctionEvent.Start(level, toActiveBeacon(transition.start())));
        }
        if (transition.stop() != null || transition.start() != null) {
            AreaSync.markChanged(level);
        }
    }

    private static Optional<ResourceKey<BeaconFunction>> readFunction(BeaconBlockEntity beacon) {
        return beacon.getExistingData(ModRegistries.BEACON_FUNCTION)
                .filter(ModRegistries.FUNCTIONS::containsKey)
                .map(BeaconTracker::functionKey);
    }

    private static ResourceKey<BeaconFunction> functionKey(ResourceLocation id) {
        return ResourceKey.create(BeaconholdApi.FUNCTION_REGISTRY_KEY, id);
    }

    private static ActiveBeacon toActiveBeacon(BeaconRecord beacon) {
        int radius = BeaconholdConfig.SERVER.radius(beacon.pyramidLevel());
        return new ActiveBeacon(
                beacon.pos(),
                beacon.pyramidLevel(),
                functionKey(beacon.function()),
                new BeaconArea(beacon.pos(), radius));
    }

    /**
     * Gives the active beacons with a function that cover a position.
     *
     * @param level the dimension
     * @param pos the position
     * @param function the function
     * @return the beacons whose area contains the position
     */
    public static List<ActiveBeacon> beaconsCovering(
            ServerLevel level, BlockPos pos, ResourceKey<BeaconFunction> function) {
        List<ActiveBeacon> result = new ArrayList<>();
        for (BeaconRecord beacon : BeaconSavedData.get(level).all()) {
            if (covers(beacon, pos, function)) {
                result.add(toActiveBeacon(beacon));
            }
        }
        return result;
    }

    /**
     * Tells if an active beacon with a function covers a position.
     *
     * @param level the dimension
     * @param pos the position
     * @param function the function
     * @return true if at least one beacon covers the position
     */
    public static boolean isCovered(ServerLevel level, BlockPos pos, ResourceKey<BeaconFunction> function) {
        for (BeaconRecord beacon : BeaconSavedData.get(level).all()) {
            if (covers(beacon, pos, function)) {
                return true;
            }
        }
        return false;
    }

    private static boolean covers(BeaconRecord beacon, BlockPos pos, ResourceKey<BeaconFunction> function) {
        return beacon.active()
                && beacon.function().equals(function.location())
                && new BeaconArea(beacon.pos(), BeaconholdConfig.SERVER.radius(beacon.pyramidLevel())).contains(pos);
    }

    /**
     * Gives all the active beacons of a dimension.
     *
     * @param level the dimension
     * @return the active beacons
     */
    public static List<ActiveBeacon> activeBeacons(ServerLevel level) {
        List<ActiveBeacon> result = new ArrayList<>();
        for (BeaconRecord beacon : BeaconSavedData.get(level).all()) {
            if (beacon.active()) {
                result.add(toActiveBeacon(beacon));
            }
        }
        return result;
    }

    /**
     * Gives the saved state of a beacon.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @return the record of the beacon, or an empty value if the beacon has no function
     */
    public static Optional<BeaconRecord> getRecord(ServerLevel level, BlockPos pos) {
        return Optional.ofNullable(BeaconSavedData.get(level).get(pos));
    }

    /**
     * Gives the function of a beacon. The block entity gives the function in a loaded chunk. The saved list gives the
     * function in a chunk that is not loaded.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @return the function, or an empty value
     */
    public static Optional<ResourceKey<BeaconFunction>> getFunction(ServerLevel level, BlockPos pos) {
        if (level.isLoaded(pos)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            return blockEntity instanceof BeaconBlockEntity beacon ? readFunction(beacon) : Optional.empty();
        }
        return getRecord(level, pos).map(beacon -> functionKey(beacon.function()));
    }

    /**
     * Sets the function of a beacon in a loaded chunk, and updates its record at once.
     *
     * @param level the dimension
     * @param pos the position of the beacon
     * @param function the new function, or {@code null} to remove the function
     * @return true if there is a beacon at the position
     * @throws IllegalArgumentException if the registry does not contain the function
     */
    public static boolean setFunction(ServerLevel level, BlockPos pos, @Nullable ResourceKey<BeaconFunction> function) {
        if (function != null && !ModRegistries.FUNCTIONS.containsKey(function)) {
            throw new IllegalArgumentException("Unknown beacon function: " + function.location());
        }
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BeaconBlockEntity beacon)) {
            return false;
        }
        if (function == null) {
            beacon.removeData(ModRegistries.BEACON_FUNCTION);
        } else {
            beacon.setData(ModRegistries.BEACON_FUNCTION, function.location());
        }
        update(level, beacon);
        return true;
    }
}
