package io.github.nicolasgoyon.beaconhold.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class ChunkRulesTest {
    private static final ChunkPos CENTER = new ChunkPos(10, -4);

    @Test
    void defaultCountsGoUpTo13() {
        int[] counts = ChunkRules.defaultChunks();
        assertEquals(0, ChunkRules.chunkCount(0, counts));
        assertEquals(1, ChunkRules.chunkCount(1, counts));
        assertEquals(5, ChunkRules.chunkCount(2, counts));
        assertEquals(9, ChunkRules.chunkCount(3, counts));
        assertEquals(13, ChunkRules.chunkCount(4, counts));
        assertEquals(13, ChunkRules.chunkCount(5, counts));
    }

    @Test
    void firstChunkIsTheChunkOfTheBeacon() {
        assertEquals(List.of(CENTER), ChunkRules.nearestChunks(CENTER, 1));
    }

    @Test
    void fiveChunksMakeACross() {
        assertEquals(
                Set.of(CENTER, offset(-1, 0), offset(1, 0), offset(0, -1), offset(0, 1)),
                new HashSet<>(ChunkRules.nearestChunks(CENTER, 5)));
    }

    @Test
    void nineChunksMakeASquare() {
        Set<ChunkPos> square = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                square.add(offset(dx, dz));
            }
        }
        assertEquals(square, new HashSet<>(ChunkRules.nearestChunks(CENTER, 9)));
    }

    @Test
    void thirteenChunksMakeACircleOfRadius2() {
        Set<ChunkPos> circle = new HashSet<>(ChunkRules.nearestChunks(CENTER, 9));
        circle.addAll(List.of(offset(-2, 0), offset(2, 0), offset(0, -2), offset(0, 2)));
        assertEquals(circle, new HashSet<>(ChunkRules.nearestChunks(CENTER, 13)));
    }

    @Test
    void chunksAreInTheOrderOfTheDistance() {
        List<ChunkPos> chunks = ChunkRules.nearestChunks(CENTER, 49);
        assertEquals(49, chunks.size());
        assertEquals(49, new HashSet<>(chunks).size());
        int last = 0;
        for (ChunkPos chunk : chunks) {
            int dx = chunk.x - CENTER.x;
            int dz = chunk.z - CENTER.z;
            int distance = dx * dx + dz * dz;
            assertTrue(distance >= last, "The order must follow the distance");
            last = distance;
        }
    }

    @Test
    void noChunkForACountOfZero() {
        assertTrue(ChunkRules.nearestChunks(CENTER, 0).isEmpty());
    }

    @Test
    void limitOfTheServerReducesTheChunks() {
        assertEquals(13, ChunkRules.allowedChunks(13, 0, 256));
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
}
