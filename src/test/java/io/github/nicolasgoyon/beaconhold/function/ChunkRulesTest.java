package io.github.nicolasgoyon.beaconhold.function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.nicolasgoyon.beaconhold.beacon.BeaconRecord;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ChunkRulesTest {
    private static final ChunkPos CENTER = new ChunkPos(10, -4);

    /** The radii of the area for the pyramid levels 1 to 4. */
    private static final int[] RADII = {20, 47, 73, 100};

    /** The smallest and the largest numbers of chunks for each pyramid level, for all the positions in a chunk. */
    private static final int[][] COUNTS = {{9, 13}, {36, 41}, {81, 88}, {144, 151}};

    @Test
    void chunkCountsForEachPyramidLevel() {
        for (int level = 0; level < RADII.length; level++) {
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int count = ChunkRules.areaChunks(x, z, RADII[level]).size();
                    min = Math.min(min, count);
                    max = Math.max(max, count);
                }
            }
            assertEquals(COUNTS[level][0], min, "Minimum for the level " + (level + 1));
            assertEquals(COUNTS[level][1], max, "Maximum for the level " + (level + 1));
        }
    }

    @Test
    void beaconInTheCenterOfAChunk() {
        assertEquals(9, ChunkRules.areaChunks(8, 8, 20).size());
        assertEquals(145, ChunkRules.areaChunks(8, 8, 100).size());
    }

    @Test
    void defaultLimitHoldsThreeBeaconsOfLevel4() {
        assertTrue(ChunkRules.DEFAULT_MAX_LOADED_CHUNKS >= 3 * COUNTS[3][1]);
        assertTrue(ChunkRules.DEFAULT_MAX_LOADED_CHUNKS < 4 * COUNTS[3][0]);
    }

    @Test
    void firstChunkIsTheChunkOfTheBeacon() {
        int x = CENTER.getMinBlockX() + 3;
        int z = CENTER.getMinBlockZ() + 12;
        assertEquals(CENTER, ChunkRules.areaChunks(x, z, 47).get(0));
        assertEquals(List.of(CENTER), ChunkRules.areaChunks(x, z, 0));
    }

    @Test
    void chunksAreTheChunksThatTheAreaTouches() {
        int x = CENTER.getMinBlockX() + 5;
        int z = CENTER.getMinBlockZ() + 14;
        int radius = 47;
        Set<ChunkPos> chunks = new HashSet<>(ChunkRules.areaChunks(x, z, radius));
        for (int cx = CENTER.x - 5; cx <= CENTER.x + 5; cx++) {
            for (int cz = CENTER.z - 5; cz <= CENTER.z + 5; cz++) {
                ChunkPos chunk = new ChunkPos(cx, cz);
                boolean touched = false;
                for (int bx = chunk.getMinBlockX(); bx <= chunk.getMaxBlockX() && !touched; bx++) {
                    for (int bz = chunk.getMinBlockZ(); bz <= chunk.getMaxBlockZ() && !touched; bz++) {
                        touched = (long) (bx - x) * (bx - x) + (long) (bz - z) * (bz - z) <= (long) radius * radius;
                    }
                }
                assertEquals(touched, chunks.contains(chunk), chunk.toString());
            }
        }
    }

    @Test
    void chunksAreInTheOrderOfTheDistance() {
        List<ChunkPos> chunks = ChunkRules.areaChunks(CENTER.getMinBlockX(), CENTER.getMinBlockZ(), 100);
        assertEquals(chunks.size(), new HashSet<>(chunks).size());
        int last = -1;
        for (ChunkPos chunk : chunks) {
            int distance = (chunk.x - CENTER.x) * (chunk.x - CENTER.x) + (chunk.z - CENTER.z) * (chunk.z - CENTER.z);
            assertTrue(distance >= last, "The order must follow the distance");
            last = distance;
        }
    }

    @Test
    void negativeRadiusGivesNoChunk() {
        assertTrue(ChunkRules.areaChunks(0, 0, -1).isEmpty());
    }

    @Test
    void limitOfTheServerReducesTheChunks() {
        assertEquals(13, ChunkRules.allowedChunks(13, 0, 256));
        assertEquals(151, ChunkRules.allowedChunks(151, 0, ChunkRules.DEFAULT_MAX_LOADED_CHUNKS));
        assertEquals(3, ChunkRules.allowedChunks(13, 253, 256));
        assertEquals(0, ChunkRules.allowedChunks(13, 256, 256));
        assertEquals(0, ChunkRules.allowedChunks(13, 300, 256));
        assertEquals(0, ChunkRules.allowedChunks(13, 0, 0));
    }

    @Test
    void ticketsThatTheSavedListDoesNotContainAreInvalid() {
        long a = CENTER.toLong();
        long b = offset(1, 0).toLong();
        long c = offset(2, 0).toLong();
        LongSet tickets = new LongOpenHashSet(new long[] {a, b, c});
        assertEquals(new LongOpenHashSet(new long[] {c}),
                ChunkRules.invalidChunks(tickets, new LongOpenHashSet(new long[] {a, b})));
        assertEquals(tickets, ChunkRules.invalidChunks(tickets, null));
        assertTrue(ChunkRules.invalidChunks(tickets, tickets).isEmpty());
    }

    private static ChunkPos offset(int dx, int dz) {
        return new ChunkPos(CENTER.x + dx, CENTER.z + dz);
    }

    @Test
    void onlyAnActiveBeaconWithTheFunctionKeepsItsChunks() {
        ResourceLocation loadedChunks = ResourceLocation.fromNamespaceAndPath("beaconhold", "loaded_chunks");
        ResourceLocation other = ResourceLocation.fromNamespaceAndPath("beaconhold", "no_hostile_spawns");
        BlockPos pos = new BlockPos(3, 64, -7);
        assertTrue(ChunkRules.keepsChunks(new BeaconRecord(pos, 2, true, loadedChunks), loadedChunks));
        assertFalse(ChunkRules.keepsChunks(new BeaconRecord(pos, 2, false, loadedChunks), loadedChunks));
        assertFalse(ChunkRules.keepsChunks(new BeaconRecord(pos, 2, true, other), loadedChunks));
        assertFalse(ChunkRules.keepsChunks(null, loadedChunks));
    }

    @Test
    void allocationFollowsTheOrderOfTheBeacons() {
        assertArrayEquals(new int[] {145, 145, 145}, ChunkRules.allocate(new int[] {145, 145, 145}, 512));
        assertArrayEquals(new int[] {145, 145, 145, 77}, ChunkRules.allocate(new int[] {145, 145, 145, 145}, 512));
        assertArrayEquals(new int[] {145, 55, 0}, ChunkRules.allocate(new int[] {145, 145, 9}, 200));
        assertArrayEquals(new int[] {0, 0}, ChunkRules.allocate(new int[] {9, 9}, 0));
        assertArrayEquals(new int[0], ChunkRules.allocate(new int[0], 512));
    }

    @Test
    void aHigherLimitGivesNoBeaconFewerChunks() {
        int[] wanted = {41, 9, 151, 85, 13};
        int[] before = ChunkRules.allocate(wanted, 0);
        for (int max = 1; max <= 320; max++) {
            int[] after = ChunkRules.allocate(wanted, max);
            int total = 0;
            for (int i = 0; i < wanted.length; i++) {
                assertTrue(after[i] >= before[i], "Beacon " + i + " with the limit " + max);
                assertTrue(after[i] <= wanted[i], "Beacon " + i + " with the limit " + max);
                total += after[i];
            }
            assertTrue(total <= max, "The total stays in the limit " + max);
            before = after;
        }
    }
}
