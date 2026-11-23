package io.github.nicolasgoyon.beaconhold.beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** The saved list of the beacons with a function, for one dimension. */
public final class BeaconSavedData extends SavedData {
    private static final String NAME = "beaconhold_beacons";
    private static final String TAG_BEACONS = "beacons";

    private final Map<BlockPos, BeaconRecord> beacons = new LinkedHashMap<>();

    /**
     * Gives the saved list of a dimension. The dimension makes a new list when it has no list.
     *
     * @param level the dimension
     * @return the saved list
     */
    public static BeaconSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(BeaconSavedData::new, BeaconSavedData::load), NAME);
    }

    private static BeaconSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        BeaconSavedData data = new BeaconSavedData();
        ListTag list = tag.getList(TAG_BEACONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            BeaconRecord beacon = BeaconRecord.load(list.getCompound(i));
            if (beacon != null) {
                data.beacons.put(beacon.pos(), beacon);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (BeaconRecord beacon : beacons.values()) {
            list.add(beacon.save());
        }
        tag.put(TAG_BEACONS, list);
        return tag;
    }

    /**
     * Gives the record of a beacon.
     *
     * @param pos the position of the beacon
     * @return the record, or {@code null} if the list has no beacon at this position
     */
    @Nullable
    public BeaconRecord get(BlockPos pos) {
        return beacons.get(pos);
    }

    /**
     * Gives all the records.
     *
     * @return a view of the records. The view cannot change the list.
     */
    public Collection<BeaconRecord> all() {
        return Collections.unmodifiableCollection(beacons.values());
    }

    /**
     * Gives a copy of the records.
     *
     * @return a new list of the records
     */
    public List<BeaconRecord> copy() {
        return new ArrayList<>(beacons.values());
    }

    /**
     * Sets or removes the record of a beacon.
     *
     * @param pos the position of the beacon
     * @param beacon the new record, or {@code null} to remove the record
     */
    public void set(BlockPos pos, @Nullable BeaconRecord beacon) {
        if (beacon == null) {
            beacons.remove(pos);
        } else {
            beacons.put(beacon.pos(), beacon);
        }
        setDirty();
    }
}
