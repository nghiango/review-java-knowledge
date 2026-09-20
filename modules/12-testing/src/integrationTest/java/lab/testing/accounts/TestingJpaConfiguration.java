package lab.testing.accounts;

import lab.testsupport.SharedPostgresContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Publishes the shared PostgreSQL container to the JPA slice.
 *
 * <p>The container comes from {@link SharedPostgresContainer#instance()} and is started manually,
 * so it deliberately carries no {@code @Container}/{@code @Testcontainers} lifecycle of its own.
 * Exposing it as a {@code @ServiceConnection} bean lets Spring Boot derive the DataSource (URL,
 * username, password, driver) from the running container instead of a hand-written
 * {@code @DynamicPropertySource}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestingJpaConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return SharedPostgresContainer.instance();
    }
}
