package lab.testsupport;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * A single PostgreSQL container shared by every integration test in the JVM.
 *
 * <p>Starting a container per test class is the slowest part of a Docker-backed suite, so the
 * modules share one started instance instead. The container is started lazily on the first call to
 * {@link #instance()} and stopped by Testcontainers' Ryuk reaper (or the JVM shutdown hook) when
 * the JVM exits; there is no per-class lifecycle to manage.
 *
 * <p>Because this class starts the container itself, a consumer must <strong>not</strong> annotate
 * it with {@code @Container}/{@code @Testcontainers} — that would try to manage a second lifecycle.
 * Publish it from the test instead:
 *
 * <pre>{@code
 * @TestConfiguration(proxyBeanMethods = false)
 * class PostgresConfiguration {
 *     @Bean
 *     @ServiceConnection
 *     PostgreSQLContainer<?> postgres() {
 *         return SharedPostgresContainer.instance();
 *     }
 * }
 * }</pre>
 *
 * <p>The image is pinned to {@code postgres:17-alpine} rather than a floating tag so every module
 * tests against the same PostgreSQL major version the rest of the repository targets.
 */
public final class SharedPostgresContainer {

    private static final String IMAGE = "postgres:17-alpine";

    private static PostgreSQLContainer<?> instance;

    private SharedPostgresContainer() {}

    /**
     * Returns the shared, started PostgreSQL container, starting it on first use.
     *
     * <p>Synchronised so that parallel test execution (or two test classes discovering the
     * container at once) cannot start two containers or observe a half-started one.
     */
    public static synchronized PostgreSQLContainer<?> instance() {
        if (instance == null) {
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>(IMAGE);
            container.start();
            instance = container;
        }
        return instance;
    }
}
