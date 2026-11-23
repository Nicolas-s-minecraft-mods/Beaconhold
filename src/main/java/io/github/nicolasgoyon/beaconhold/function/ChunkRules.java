package io.github.nicolasgoyon.beaconhold.function;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;

/** The rules of the function "loaded chunks": the chunks of a beacon and the limit of the server. */
public final class ChunkRules {
    /** The default numbers of chunks, for the pyramid levels 1 to 4. */
    private static final int[] DEFAULT_CHUNKS = {1, 5, 9, 13};

    /** The default maximum number of chunks that all the beacons of the server load together. */
    public static final int DEFAULT_MAX_LOADED_CHUNKS = 256;

    private ChunkRules() {
    }

    /**
     * Gives a copy of the default numbers of chunks.
     *
     * @return the default numbers of chunks, for the pyramid levels 1 to 4
     */
    public static int[] defaultChunks() {
        return DEFAULT_CHUNKS.clone();
    }

    /**
     * Gives the number of chunks for a pyramid level.
     *
     * @param pyramidLevel the pyramid level
     * @param counts the numbers of chunks for the pyramid levels 1 to {@code counts.length}
     * @return 0 for a pyramid level of 0 or less. The last number for a pyramid level above the last level.
     */
    public static int chunkCount(int pyramidLevel, int[] counts) {
        if (pyramidLevel <= 0 || counts.length == 0) {
            return 0;
        }
        return Math.max(0, counts[Math.min(pyramidLevel, counts.length) - 1]);
    }

    /**
     * Gives the chunks nearest to a center chunk, the nearest first.
     *
     * <p>The distance is the distance between the chunk coordinates. Chunks at the same distance are in the order of
     * their X coordinate, then of their Z coordinate.
     *
     * @param center the chunk of the beacon
     * @param count the number of chunks
     * @return a list of {@code count} chunks. The first chunk is the center.
     */
    public static List<ChunkPos> nearestChunks(ChunkPos center, int count) {
        if (count <= 0) {
            return List.of();
        }
        int radius = (int) Math.ceil(Math.sqrt(count)) + 1;
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                offsets.add(new int[] {dx, dz});
            }
        }
        offsets.sort(Comparator.<int[]>comparingInt(o -> o[0] * o[0] + o[1] * o[1])
                .thenComparingInt(o -> o[0])
                .thenComparingInt(o -> o[1]));
        List<ChunkPos> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int[] offset = offsets.get(i);
            result.add(new ChunkPos(center.x + offset[0], center.z + offset[1]));
        }
        return result;
    }

    /**
     * Gives the number of chunks that a beacon can load, with the limit of the server.
     *
     * @param wanted the number of chunks for the pyramid level of the beacon
     * @param usedByOthers the number of chunks that the other beacons of the server load
     * @param max the maximum number of chunks for all the beacons of the server
     * @return a number from 0 to {@code wanted}
     */
    public static int allowedChunks(int wanted, int usedByOthers, int max) {
        return Math.max(0, Math.min(wanted, max - usedByOthers));
    }

    /**
     * Gives the chunks of a ticket that the saved list of a beacon does not contain. The server removes these tickets
     * when it loads a world.
     *
     * @param ticketChunks the chunks of the tickets of the beacon
     * @param savedChunks the chunks of the saved list of the beacon, or {@code null} if the list has no beacon at this
     *     position
     * @return the chunks to release
     */
    public static LongSet invalidChunks(LongSet ticketChunks, @Nullable LongSet savedChunks) {
        LongSet result = new LongOpenHashSet(ticketChunks);
        if (savedChunks != null) {
            result.removeAll(savedChunks);
        }
        return result;
    }
}
