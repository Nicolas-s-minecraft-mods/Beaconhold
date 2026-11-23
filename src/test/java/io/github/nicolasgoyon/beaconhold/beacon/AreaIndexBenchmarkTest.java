package io.github.nicolasgoyon.beaconhold.beacon;

import io.github.nicolasgoyon.beaconhold.api.BeaconArea;
import io.github.nicolasgoyon.beaconhold.config.AreaRadii;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Measures the cost of a query "is a position in the area of an active beacon with a function". It compares the full
 * scan of the records and the spatial index. The test runs only with the environment variable
 * {@code BEACONHOLD_BENCHMARK=true}:
 *
 * <pre>BEACONHOLD_BENCHMARK=true ./gradlew test --tests '*AreaIndexBenchmarkTest' --rerun</pre>
 *
 * <p>The result is in the standard output of the test report ({@code build/test-results/test}).
 *
 * <p>The beacons are on a square of 20,000 blocks, with a random pyramid level and the default radii. 90 % of the
 * beacons are active. Half of them have the function of the query.
 */
@EnabledIfEnvironmentVariable(named = "BEACONHOLD_BENCHMARK", matches = "true")
class AreaIndexBenchmarkTest {
    private static final ResourceLocation FUNCTION = ResourceLocation.fromNamespaceAndPath("beaconhold", "a");
    private static final ResourceLocation OTHER = ResourceLocation.fromNamespaceAndPath("beaconhold", "b");
    private static final int SIDE = 20_000;
    private static final int QUERIES = 200_000;

    @Test
    void measureTheQueries() {
        System.out.println("beacons | full scan (ns/query) | index (ns/query) | covered");
        for (int count : new int[] {10, 100, 1_000, 10_000}) {
            Random random = new Random(count);
            int[] radii = AreaRadii.defaultRadii();
            List<BeaconRecord> records = new ArrayList<>();
            AreaIndex<BeaconRecord> index = new AreaIndex<>();
            for (int i = 0; i < count; i++) {
                BlockPos pos = new BlockPos(random.nextInt(SIDE) - SIDE / 2, 64, random.nextInt(SIDE) - SIDE / 2);
                BeaconRecord beacon = new BeaconRecord(
                        pos, 1 + random.nextInt(4), random.nextInt(10) != 0, random.nextBoolean() ? FUNCTION : OTHER);
                records.add(beacon);
                if (beacon.active()) {
                    index.put(pos.asLong(), pos.getX(), pos.getZ(), AreaRadii.radius(beacon.pyramidLevel(), radii), beacon);
                }
            }
            BlockPos[] queries = new BlockPos[QUERIES];
            for (int q = 0; q < QUERIES; q++) {
                queries[q] = new BlockPos(random.nextInt(SIDE) - SIDE / 2, 64, random.nextInt(SIDE) - SIDE / 2);
            }
            for (int warmup = 0; warmup < 3; warmup++) {
                scan(records, queries, radii);
                indexed(index, queries);
            }
            long start = System.nanoTime();
            int scanned = scan(records, queries, radii);
            long scanTime = System.nanoTime() - start;
            start = System.nanoTime();
            int found = indexed(index, queries);
            long indexTime = System.nanoTime() - start;
            if (scanned != found) {
                throw new AssertionError("The two methods give different results: " + scanned + ", " + found);
            }
            System.out.println(String.format(
                    Locale.ROOT,
                    "%d | %.1f | %.1f | %d",
                    count,
                    scanTime / (double) QUERIES,
                    indexTime / (double) QUERIES,
                    found));
        }
    }

    /** The full scan: the same checks as the queries without the index, with the radius from the options. */
    private static int scan(List<BeaconRecord> records, BlockPos[] queries, int[] radii) {
        int covered = 0;
        for (BlockPos pos : queries) {
            for (BeaconRecord beacon : records) {
                if (beacon.active()
                        && beacon.function().equals(FUNCTION)
                        && new BeaconArea(beacon.pos(), AreaRadii.radius(beacon.pyramidLevel(), radii.clone()))
                                .contains(pos)) {
                    covered++;
                    break;
                }
            }
        }
        return covered;
    }

    private static int indexed(AreaIndex<BeaconRecord> index, BlockPos[] queries) {
        int covered = 0;
        for (BlockPos pos : queries) {
            for (BeaconRecord beacon : index.containing(pos.getX(), pos.getZ())) {
                if (beacon.function().equals(FUNCTION)) {
                    covered++;
                    break;
                }
            }
        }
        return covered;
    }
}
