package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.api.BeaconFunction;
import io.github.nicolasgoyon.beaconhold.config.BeaconholdConfig;
import io.github.nicolasgoyon.beaconhold.mixin.BeaconBlockEntityAccessor;
import io.github.nicolasgoyon.beaconhold.network.AreaSync;
import io.github.nicolasgoyon.beaconhold.registry.ModRegistries;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

/**
 * Keeps, for each dimension, the loaded beacons that show the energy dome (see {@link DomeRules}). The list is in
 * memory only. Each beacon updates its entry at the interval of the vanilla powers.
 */
public final class BeamBeacons {
    /** For each dimension: the position of each beacon (as a long) and its pyramid level. The server thread uses it. */
    private static final Map<ServerLevel, Long2IntMap> BEACONS = new WeakHashMap<>();

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
        boolean shows = DomeRules.showsDome(
                pyramidLevel,
                !beacon.getBeamSections().isEmpty(),
                !BeaconPower.isOff(beacon),
                showsArea,
                BeaconholdConfig.SERVER.vanillaPowersUseArea());
        long pos = beacon.getBlockPos().asLong();
        Long2IntMap beacons = BEACONS.computeIfAbsent(level, key -> new Long2IntOpenHashMap());
        int before = beacons.getOrDefault(pos, 0);
        int after = shows ? pyramidLevel : 0;
        if (before == after) {
            return;
        }
        if (after == 0) {
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
        Long2IntMap beacons = BEACONS.get(level);
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
        Long2IntMap beacons = BEACONS.get(level);
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
     * Gives the beacons of a dimension that show the energy dome.
     *
     * @param level the dimension
     * @return a copy: the position of each beacon (as a long) and its pyramid level
     */
    public static Long2IntMap of(ServerLevel level) {
        Long2IntMap beacons = BEACONS.get(level);
        return beacons == null ? new Long2IntOpenHashMap() : new Long2IntOpenHashMap(beacons);
    }
}
