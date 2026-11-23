package io.github.nicolasgoyon.beaconhold.function;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconRecord;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

/** The rules of the function "loaded chunks": the chunks of a beacon and the limit of the server. */
public final class ChunkRules {
    /**
     * The default maximum number of chunks that all the beacons of the server load together. A beacon with the pyramid
     * level 4 and the default radius loads 85 to 91 chunks, so this value is enough for 5 such beacons.
     */
    public static final int DEFAULT_MAX_LOADED_CHUNKS = 512;

    /**
     * The default maximum number of chunks that the beacons of one player load together. A beacon with the pyramid level
     * 4 and the default radius loads 85 to 91 chunks, so this value is enough for 2 such beacons.
     */
    public static final int DEFAULT_MAX_CHUNKS_PER_PLAYER = 256;

    private ChunkRules() {
    }

    /**
     * Gives the chunks that the area of a beacon touches, the nearest first.
     *
     * <p>A chunk is in the list when at least one column of blocks of the chunk is in the area. The order follows the
     * distance from the chunk of the beacon, then the X coordinate, then the Z coordinate.
     *
     * @param x the X coordinate of the beacon
     * @param z the Z coordinate of the beacon
     * @param radius the radius of the area in blocks
     * @return the chunks. A radius under 0 gives no chunk.
     */
    public static List<ChunkPos> areaChunks(int x, int z, int radius) {
        if (radius < 0) {
            return List.of();
        }
        ChunkPos center = new ChunkPos(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        long r2 = (long) radius * radius;
        List<ChunkPos> result = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(x - radius); cx <= SectionPos.blockToSectionCoord(x + radius); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(z - radius);
                    cz <= SectionPos.blockToSectionCoord(z + radius);
                    cz++) {
                long dx = (long) nearest(x, cx) - x;
                long dz = (long) nearest(z, cz) - z;
                if (dx * dx + dz * dz <= r2) {
                    result.add(new ChunkPos(cx, cz));
                }
            }
        }
        result.sort(Comparator.<ChunkPos>comparingInt(c -> square(c.x - center.x) + square(c.z - center.z))
                .thenComparingInt(c -> c.x)
                .thenComparingInt(c -> c.z));
        return result;
    }

    /** Gives the coordinate of the chunk that is nearest to a block coordinate. */
    private static int nearest(int block, int chunk) {
        int min = SectionPos.sectionToBlockCoord(chunk);
        return Math.max(min, Math.min(block, min + 15));
    }

    private static int square(int value) {
        return value * value;
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
     * Gives the number of chunks that a beacon can load, with the limit of the server and the limit for each player.
     *
     * @param wanted the number of chunks for the pyramid level of the beacon
     * @param usedByOthers the number of chunks that the other beacons of the server load
     * @param max the maximum number of chunks for all the beacons of the server
     * @param hasOwner true if the beacon has a known owner. A beacon with no known owner has only the limit of the server.
     * @param usedByOwner the number of chunks that the other beacons of the owner load
     * @param maxPerPlayer the maximum number of chunks for all the beacons of one player
     * @return a number from 0 to {@code wanted}
     */
    public static int allowedChunks(
            int wanted, int usedByOthers, int max, boolean hasOwner, int usedByOwner, int maxPerPlayer) {
        int allowed = allowedChunks(wanted, usedByOthers, max);
        return hasOwner ? allowedChunks(allowed, usedByOwner, maxPerPlayer) : allowed;
    }

    /**
     * Gives the number of chunks that each beacon loads, with the limit of the server. The beacons get their chunks in
     * the order of the list: a beacon gets the capacity that the beacons before it leave free.
     *
     * @param wanted the number of chunks that the area of each beacon touches, in the order of the beacons
     * @param max the maximum number of chunks for all the beacons of the server
     * @return the number of chunks for each beacon, in the same order
     */
    public static int[] allocate(int[] wanted, int max) {
        int[] result = new int[wanted.length];
        int used = 0;
        for (int i = 0; i < wanted.length; i++) {
            result[i] = allowedChunks(wanted[i], used, max);
            used += result[i];
        }
        return result;
    }

    /**
     * Gives the number of chunks that each beacon loads, with the limit of the server and the limit for each player. The
     * beacons get their chunks in the order of the list: a beacon gets the capacity that the beacons before it leave
     * free. A beacon with no known owner has only the limit of the server.
     *
     * @param wanted the number of chunks that the area of each beacon touches, in the order of the beacons
     * @param owners the owner of each beacon, in the same order. A {@code null} value is a beacon with no known owner.
     * @param max the maximum number of chunks for all the beacons of the server
     * @param maxPerPlayer the maximum number of chunks for all the beacons of one player
     * @return the number of chunks for each beacon, in the same order
     */
    public static int[] allocate(int[] wanted, List<UUID> owners, int max, int maxPerPlayer) {
        if (owners.size() != wanted.length) {
            throw new IllegalArgumentException("Each beacon needs one owner value");
        }
        int[] result = new int[wanted.length];
        Map<UUID, Integer> usedByOwner = new HashMap<>();
        int used = 0;
        for (int i = 0; i < wanted.length; i++) {
            UUID owner = owners.get(i);
            int ownerUsed = owner == null ? 0 : usedByOwner.getOrDefault(owner, 0);
            result[i] = allowedChunks(wanted[i], used, max, owner != null, ownerUsed, maxPerPlayer);
            used += result[i];
            if (owner != null) {
                usedByOwner.put(owner, ownerUsed + result[i]);
            }
        }
        return result;
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

    /**
     * Tells if a beacon keeps its saved chunks when the server starts.
     *
     * @param beacon the record of the beacon in the saved list of the beacons, or {@code null} if the list has no beacon
     *     at this position
     * @param loadedChunks the id of the function "loaded chunks"
     * @return true if the beacon is active with the function "loaded chunks"
     */
    public static boolean keepsChunks(@Nullable BeaconRecord beacon, ResourceLocation loadedChunks) {
        return beacon != null && beacon.active() && beacon.function().equals(loadedChunks);
    }
}
