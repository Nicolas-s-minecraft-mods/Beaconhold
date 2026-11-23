package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.mixin.BeaconBlockEntityAccessor;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * Keeps, for each dimension, the loaded beacons that show the energy dome when the server option
 * {@code area.vanillaPowersUseArea} is on (see {@link DomeRules}). The list is in memory only. Each beacon updates its
 * entry at the interval of the vanilla powers.
 *
 * <p>A beacon with no function shows the dome only when the option is on. {@link #of} reads the option at each call, so
 * a change of the option changes the domes at once.
 */
public final class BeamBeacons {
    /** For each dimension: the position of each beacon (as a long) and its entry. The server thread uses it. */
    private static final Map<ServerLevel, Long2ObjectMap<Beam>> BEACONS = new WeakHashMap<>();

    /**
     * The entry of a beacon in the list.
     *
     * @param pyramidLevel the pyramid level of the beacon, 1 or more
     * @param noFunction true if the beacon has no function
     */
    private record Beam(int pyramidLevel, boolean noFunction) {
        boolean showsDome(boolean vanillaPowersUseArea) {
            return DomeRules.showsDome(
                    pyramidLevel, true, true, noFunction ? null : Boolean.TRUE, vanillaPowersUseArea);
        }
    }

    private BeamBeacons() {
    }

    /**
     * Updates the entry of a loaded beacon. The beacon must know its pyramid level and its beam.
     *
     * @param level the dimension of the beacon
     * @param beacon the block entity of the beacon
     */
    public static void update(ServerLevel level, BeaconBlockEntity beacon) {
        int pyramidLevel = ((BeaconBlockEntityAccessor) beacon).getBeaconholdLevels();
        Boolean showsArea = beacon.getExistingData(ModRegistries.BEACON_FUNCTION)
                .map(BeamBeacons::showsArea)
                .orElse(null);
        // The list keeps the beacons with no function. The method "of" applies the current value of the option.
        boolean shows = DomeRules.showsDome(
                pyramidLevel, !beacon.getBeamSections().isEmpty(), !BeaconPower.isOff(beacon), showsArea, true);
        long pos = beacon.getBlockPos().asLong();
        Long2ObjectMap<Beam> beacons = BEACONS.computeIfAbsent(level, key -> new Long2ObjectOpenHashMap<>());
        Beam before = beacons.get(pos);
        Beam after = shows ? new Beam(pyramidLevel, showsArea == null) : null;
        if (Objects.equals(before, after)) {
            return;
        }
        if (after == null) {
            beacons.remove(pos);
        } else {
            beacons.put(pos, after);
        }
        AreaSync.markChanged(level);
    }

    private static boolean showsArea(ResourceLocation id) {
        BeaconFunction function = ModRegistries.FUNCTIONS.get(id);
        return function == null || function.showsArea();
    }

    /**
     * Removes the entry of a beacon.
     *
     * @param level the dimension of the beacon
     * @param pos the position of the beacon
     */
    public static void remove(ServerLevel level, BlockPos pos) {
        Long2ObjectMap<Beam> beacons = BEACONS.get(level);
        if (beacons != null && beacons.containsKey(pos.asLong())) {
            beacons.remove(pos.asLong());
            AreaSync.markChanged(level);
        }
    }

    /**
     * Removes the entries of the positions that are not loaded or that have no beacon.
     *
     * @param level the dimension
     */
    public static void removeMissing(ServerLevel level) {
        Long2ObjectMap<Beam> beacons = BEACONS.get(level);
        if (beacons == null) {
            return;
        }
        boolean removed = beacons.keySet().removeIf(packed -> {
            BlockPos pos = BlockPos.of(packed);
            return !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BeaconBlockEntity);
        });
        if (removed) {
            AreaSync.markChanged(level);
        }
    }

    /**
     * Gives the beacons of a dimension that show the energy dome, with the current value of the option
     * {@code area.vanillaPowersUseArea}.
     *
     * @param level the dimension
     * @return a copy: the position of each beacon (as a long) and its pyramid level
     */
    public static Long2IntMap of(ServerLevel level) {
        Long2IntMap result = new Long2IntOpenHashMap();
        Long2ObjectMap<Beam> beacons = BEACONS.get(level);
        if (beacons == null) {
            return result;
        }
        boolean vanillaPowersUseArea = BeaconholdConfig.SERVER.vanillaPowersUseArea();
        for (Long2ObjectMap.Entry<Beam> entry : beacons.long2ObjectEntrySet()) {
            if (entry.getValue().showsDome(vanillaPowersUseArea)) {
                result.put(entry.getLongKey(), entry.getValue().pyramidLevel());
            }
        }
        return result;
    }
}
