package io.github.nicolasgoyon.beaconhold.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class LoadedChunksSavedDataTest {
    @Test
    void saveAndLoadKeepTheChunksOfEachBeacon() {
        BlockPos first = new BlockPos(10, 70, -20);
        BlockPos second = new BlockPos(-300, 5, 400);
        LongSet firstChunks = new LongOpenHashSet(new long[] {new ChunkPos(0, -2).toLong(), new ChunkPos(1, -2).toLong()});
        LongSet secondChunks = new LongOpenHashSet(new long[] {new ChunkPos(-19, 25).toLong()});
        LoadedChunksSavedData data = new LoadedChunksSavedData();
        data.set(first, firstChunks);
        data.set(second, secondChunks);

        CompoundTag tag = data.save(new CompoundTag(), RegistryAccess.EMPTY);
        LoadedChunksSavedData loaded = LoadedChunksSavedData.load(tag, RegistryAccess.EMPTY);

        assertEquals(List.of(first, second), loaded.positions());
        assertEquals(firstChunks, loaded.get(first));
        assertEquals(secondChunks, loaded.get(second));
        assertEquals(3, loaded.total());
        assertNull(loaded.get(BlockPos.ZERO));
    }
}
