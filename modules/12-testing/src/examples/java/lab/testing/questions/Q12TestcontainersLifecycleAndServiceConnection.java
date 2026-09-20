package lab.testing.questions;

import java.util.List;
import java.util.Map;

/**
 * Q12: Testcontainers lifecycle and {@code @ServiceConnection}.
 *
 * <p>The lifecycle question is "how many containers does the suite start?", and the answer decides
 * the CI bill. {@code @Testcontainers} with an instance {@code @Container} field starts and stops a
 * container around every test; a static field starts it once per class; a hand-started singleton
 * (or a {@code @TestConfiguration} bean) starts it once per JVM, which is what this module does
 * with {@code SharedPostgresContainer}. {@code @ServiceConnection} is the second half: Boot derives
 * the DataSource URL, credentials and driver from the running container, so the test never
 * hand-writes {@code @DynamicPropertySource}. Ryuk reaps whatever is left when the JVM dies.
 */
public class Q12TestcontainersLifecycleAndServiceConnection {

    /** An image and what it costs to start and to migrate the schema. */
    record Container(String image, long startupMillis, long migrationMillis) {}

    /** How a suite manages its containers. */
    enum Lifecycle {
        PER_TEST_METHOD,
        PER_TEST_CLASS,
        SINGLETON_PER_JVM
    }

    /** One lifecycle choice and how many container starts it costs. */
    record LifecyclePlan(Lifecycle lifecycle, int starts) {}

    /** A running container, as {@code @ServiceConnection} sees it. */
    record RunningContainer(String host, int port, String database, String user, String password) {}

    public static void main(String[] args) {
        Container postgres = new Container("postgres:17-alpine", 8_000, 1_500);

        int testClasses = 12;
        int testsPerClass = 8;

        List<LifecyclePlan> plans =
                List.of(
                        new LifecyclePlan(Lifecycle.PER_TEST_METHOD, testClasses * testsPerClass),
                        new LifecyclePlan(Lifecycle.PER_TEST_CLASS, testClasses),
                        new LifecyclePlan(Lifecycle.SINGLETON_PER_JVM, 1));

        long perMethodMillis = suiteMillis(plans.get(0).starts(), postgres); // 96 x 9500
        long perClassMillis = suiteMillis(plans.get(1).starts(), postgres); // 12 x 9500
        long singletonMillis = suiteMillis(plans.get(2).starts(), postgres); // 1 x 9500
        long savedMillis = perClassMillis - singletonMillis; // 104_500
        int cheapestStarts =
                plans.stream().mapToInt(LifecyclePlan::starts).min().orElseThrow(); // 1

        // @ServiceConnection derives the DataSource from the running container.
        RunningContainer running =
                new RunningContainer("localhost", 54_321, "lab", "lab", "secret");
        Map<String, String> dataSourceProperties =
                Map.of(
                        "spring.datasource.url",
                        "jdbc:postgresql://"
                                + running.host()
                                + ":"
                                + running.port()
                                + "/"
                                + running.database(),
                        "spring.datasource.username",
                        running.user(),
                        "spring.datasource.password",
                        running.password(),
                        "spring.datasource.driver-class-name",
                        "org.postgresql.Driver");
        String jdbcUrl = dataSourceProperties.get("spring.datasource.url"); // the derived URL

        // Reuse across runs is opt-in: it needs testcontainers.reuse.enable=true in the user's
        // .testcontainers.properties, otherwise every run starts a fresh container.
        boolean reuseRequiresOptIn = true; // withReuse(true) alone does nothing
        boolean ryukReapsOrphans = true; // the reaper removes containers after the JVM exits

        System.out.println("Per-method: " + perMethodMillis + " ms"); // Per-method: 912000 ms
        System.out.println("Per-class: " + perClassMillis + " ms"); // Per-class: 114000 ms
        System.out.println("Singleton: " + singletonMillis + " ms"); // Singleton: 9500 ms
        System.out.println("Saved: " + savedMillis + " ms"); // Saved: 104500 ms
        System.out.println("Cheapest starts: " + cheapestStarts); // Cheapest starts: 1
        System.out.println("Derived props: " + dataSourceProperties.size()); // Derived props: 4
        System.out.println(
                "JDBC URL: " + jdbcUrl); // JDBC URL: jdbc:postgresql://localhost:54321/lab
        System.out.println("Reuse opt-in: " + reuseRequiresOptIn); // Reuse opt-in: true
        System.out.println("Ryuk reaps: " + ryukReapsOrphans); // Ryuk reaps: true
    }

    /** The wall clock of starting {@code starts} containers and migrating each schema once. */
    private static long suiteMillis(int starts, Container container) {
        return starts * (container.startupMillis() + container.migrationMillis());
    }
}
