package lab.testing.questions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Q08: What makes a test deterministic (and what makes it flaky).
 *
 * <p>A test is deterministic when every input it depends on is either a constant or injected: the
 * clock, the identifiers, the iteration order and the parallelism are the four usual leaks. The
 * assertions below are written so that their result is the same on every run — including the ones
 * about generated identifiers, which assert a *property* instead of a value.
 */
public class Q08DeterministicAndFlakyTests {

    public static void main(String[] args) {
        // 1. A wall clock makes a test true only in the window it was written in.
        long writtenAtMillis = 1_700_000_000_000L;
        long ciNowMillis = 1_700_003_600_000L; // one hour later
        boolean wallClockHolds = ciNowMillis - writtenAtMillis < 60_000; // false: time-bombed

        // 2. An injected clock removes the race entirely.
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
        boolean fixedClockIsReproducible = Instant.now(clock).equals(Instant.now(clock)); // true
        Clock advanced = Clock.offset(clock, Duration.ofHours(1));
        boolean offsetClockIsControlled =
                Instant.now(advanced).equals(Instant.parse("2026-01-01T13:00:00Z")); // true

        // 3. Generated identifiers: assert the property (distinct), never the value.
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        boolean idsAreDistinct = !firstId.equals(secondId); // true
        boolean idsAreEqual = firstId.equals(secondId); // false

        // 4. Iteration order: a Set has none, a List has one.
        boolean setsIgnoreOrder = Set.of("a", "b").equals(Set.of("b", "a")); // true
        boolean listsRespectOrder = List.of("a", "b").equals(List.of("b", "a")); // false

        // 5. A parallel reduction must not depend on encounter order.
        int parallelSum = IntStream.of(1, 2, 3, 4).parallel().sum(); // 10
        boolean sumIsOrderIndependent = parallelSum == 10; // true

        System.out.println("Wall clock ok: " + wallClockHolds); // Wall clock ok: false
        System.out.println("Fixed clock: " + fixedClockIsReproducible); // Fixed clock: true
        System.out.println("Offset clock: " + offsetClockIsControlled); // Offset clock: true
        System.out.println("Ids distinct: " + idsAreDistinct); // Ids distinct: true
        System.out.println("Ids equal: " + idsAreEqual); // Ids equal: false
        System.out.println("Sets ignore order: " + setsIgnoreOrder); // Sets ignore order: true
        System.out.println(
                "Lists respect order: " + listsRespectOrder); // Lists respect order: false
        System.out.println("Parallel sum: " + parallelSum); // Parallel sum: 10
        System.out.println("Sum order-free: " + sumIsOrderIndependent); // Sum order-free: true
    }
}
