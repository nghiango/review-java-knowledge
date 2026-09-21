package lab.java25boot4.testsupport;

import org.testcontainers.containers.PostgreSQLContainer;

/** Shared Testcontainers instance for the Java 25 & Spring Boot 4 track. */
public final class PostgresContainers {

    private static volatile PostgreSQLContainer<?> instance;

    private PostgresContainers() {}

    public static PostgreSQLContainer<?> getInstance() {
        if (instance == null) {
            synchronized (PostgresContainers.class) {
                if (instance == null) {
                    PostgreSQLContainer<?> container =
                            new PostgreSQLContainer<>("postgres:16-alpine")
                                    .withDatabaseName("track_db")
                                    .withUsername("postgres")
                                    .withPassword("postgres");
                    container.start();
                    instance = container;
                }
            }
        }
        return instance;
    }
}
