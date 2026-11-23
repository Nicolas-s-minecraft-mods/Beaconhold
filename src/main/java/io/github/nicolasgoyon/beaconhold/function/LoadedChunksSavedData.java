package io.github.nicolasgoyon.beaconhold.function;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** The saved list of the chunks that each beacon of a dimension loads. */
public final class LoadedChunksSavedData extends SavedData {
    private static final String NAME = "beaconhold_loaded_chunks";
    private static final String TAG_BEACONS = "beacons";
    private static final String TAG_POS = "pos";
    private static final String TAG_CHUNKS = "chunks";

    private final Map<BlockPos, LongSet> beacons = new LinkedHashMap<>();

    /**
     * Gives the saved list of a dimension. The dimension makes a new list when it has no list.
     *
     * @param level the dimension
     * @return the saved list
     */
    public static LoadedChunksSavedData get(ServerLevel level) {
        return level.getDataStorage()
                .computeIfAbsent(new Factory<>(LoadedChunksSavedData::new, LoadedChunksSavedData::load), NAME);
    }

    private static LoadedChunksSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LoadedChunksSavedData data = new LoadedChunksSavedData();
        ListTag list = tag.getList(TAG_BEACONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.beacons.put(
                    BlockPos.of(entry.getLong(TAG_POS)), new LongOpenHashSet(entry.getLongArray(TAG_CHUNKS)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        beacons.forEach((pos, chunks) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong(TAG_POS, pos.asLong());
            entry.put(TAG_CHUNKS, new LongArrayTag(chunks.toLongArray()));
            list.add(entry);
        });
        tag.put(TAG_BEACONS, list);
        return tag;
    }

    /**
     * Gives the chunks of a beacon.
     *
     * @param pos the position of the beacon
     * @return a view of the chunks, or {@code null} if the beacon loads no chunk
     */
    @Nullable
    public LongSet get(BlockPos pos) {
        LongSet chunks = beacons.get(pos);
        return chunks == null ? null : LongSets.unmodifiable(chunks);
    }

    /**
     * Sets or removes the chunks of a beacon.
     *
     * @param pos the position of the beacon
     * @param chunks the chunks, or {@code null} to remove the beacon
     */
    public void set(BlockPos pos, @Nullable LongSet chunks) {
        if (chunks == null) {
            beacons.remove(pos);
        } else {
            beacons.put(pos.immutable(), new LongOpenHashSet(chunks));
        }
        setDirty();
    }

    /**
     * Gives the number of chunks that the beacons of this dimension load.
     *
     * @return the sum of the chunks of all the beacons
     */
    public int total() {
        int total = 0;
        for (LongSet chunks : beacons.values()) {
            total += chunks.size();
        }
        return total;
    }
}
