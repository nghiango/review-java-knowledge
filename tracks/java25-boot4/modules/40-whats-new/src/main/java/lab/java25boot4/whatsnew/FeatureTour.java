package lab.java25boot4.whatsnew;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;
import java.util.stream.Gatherers;

/**
 * Runnable tour of the Java 22 → 25 and Spring Boot 3.5 → 4.0 changes covered by this module.
 *
 * <p>Run it with {@code ../../gradlew :modules:40-whats-new:runFeatureTour}.
 */
public final class FeatureTour {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    private FeatureTour() {}

    public static void main(String[] args) {
        System.out.println("== Java 25 / Spring Boot 4 feature tour ==");
        streamGatherers();
        foreignMemory();
        scopedValues();
        migrationChecklist();
    }

    private static void streamGatherers() {
        List<Integer> samples = List.of(3, 1, 4, 1, 5, 9, 2);
        List<List<Integer>> batches = samples.stream().gather(Gatherers.windowFixed(3)).toList();
        List<Integer> runningTotals = samples.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList();
        System.out.println("gather windowFixed(3) : " + batches);
        System.out.println("gather scan(sum)      : " + runningTotals);
    }

    private static void foreignMemory() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = arena.allocate(3 * Long.BYTES);
            segment.set(ValueLayout.JAVA_LONG, 0, 42L);
            segment.set(ValueLayout.JAVA_LONG, Long.BYTES, 7L);
            System.out.println("foreign memory [0]    : " + segment.get(ValueLayout.JAVA_LONG, 0));
            System.out.println("foreign memory [1]    : " + segment.get(ValueLayout.JAVA_LONG, Long.BYTES));
        }
    }

    private static void scopedValues() {
        ScopedValue.where(REQUEST_ID, "req-42").run(() -> System.out.println("scoped value          : " + REQUEST_ID.get()));
    }

    private static void migrationChecklist() {
        System.out.println("migration checklist   :");
        System.out.println("  - delete virtual-thread pinning workarounds (synchronized no longer pins)");
        System.out.println("  - replace SecurityManager/AccessController authorization with principal-based checks");
        System.out.println("  - replace sun.misc.Unsafe memory access with java.lang.foreign");
        System.out.println("  - run with --debug and fix every removed/renamed property or auto-configuration");
        System.out.println("  - add @Validated to every @ConfigurationProperties so misconfiguration fails fast");
    }
}
