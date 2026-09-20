package lab.testing.questions;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Q12: Testcontainers lifecycle and {@code @ServiceConnection}.
 *
 * <p>The lifecycle question is "how many containers does the suite start?", and the answer decides
 * the CI bill. {@code @Testcontainers} with an instance {@code @Container} field starts and stops a
 * container around every test; a static field starts it once per class; a hand-started singleton
 * published as a {@code @ServiceConnection} bean starts it once per JVM, which is what this module
 * does. {@code @ServiceConnection} is the second half: Boot derives the DataSource URL, credentials
 * and driver from the running container, so no test hand-writes {@code @DynamicPropertySource}.
 * Constructing a container is not starting it — nothing here connects to Docker, and no container
 * is started at class-initialisation time.
 */
public class Q12TestcontainersLifecycleAndServiceConnection {

    /**
     * One container for the whole JVM, published to the context instead of started by an extension.
     */
    static final class SharedContainer {

        static final PostgreSQLContainer<?> POSTGRES =
                new PostgreSQLContainer<>("postgres:17-alpine");

        private SharedContainer() {}
    }

    /** Boot derives the DataSource from the container bean rather than from properties. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgresContainer() {
            return SharedContainer.POSTGRES;
        }
    }

    /** A static {@code @Container} field: started once for the class and shared by its tests. */
    @Testcontainers
    static class PerClassContainerTest {

        @Container
        static final PostgreSQLContainer<?> POSTGRES =
                new PostgreSQLContainer<>("postgres:17-alpine");

        @Test
        void theSchemaIsMigratedOnceForTheClass() {}
    }

    /** An instance {@code @Container} field: started and stopped around every test. */
    @Testcontainers
    static class PerMethodContainerTest {

        @Container
        final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

        @Test
        void theSchemaIsMigratedAgainForEveryTest() {}
    }

    public static void main(String[] args) {
        // Constructing a container does not start it, and the getters below only read the values
        // it was configured with. Only start() and getDockerImageName() touch Docker.
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
        int exposedPort = postgres.getExposedPorts().get(0); // 5432
        String database = postgres.getDatabaseName(); // "test"
        String username = postgres.getUsername(); // "test"

        // The image name is parsed, not resolved: parsing is pure string work.
        DockerImageName image = DockerImageName.parse("postgres:17-alpine");
        String repository = image.getRepository(); // "postgres"
        String tag = image.getVersionPart(); // "17-alpine"

        long startupMillis = 8_000; // cold start of this image on a CI agent
        long migrationMillis = 1_500; // Flyway applying the schema once
        long containerMillis = startupMillis + migrationMillis; // 9_500
        int testClasses = 12;
        int testsPerClass = 8;

        long perMethodMillis = (long) testClasses * testsPerClass * containerMillis; // 96 starts
        long perClassMillis = (long) testClasses * containerMillis; // 12 starts
        long singletonMillis = containerMillis; // 1 start
        long savedMillis = perClassMillis - singletonMillis; // 104_500

        long serviceConnectionBeans =
                Arrays.stream(ContainerConfiguration.class.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(ServiceConnection.class))
                        .count(); // 1

        String extension = Testcontainers.class.getSimpleName(); // "Testcontainers"
        String containerAnnotation = Container.class.getSimpleName(); // "Container"
        String serviceConnectionAnnotation =
                ServiceConnection.class.getSimpleName(); // "ServiceConnection"

        System.out.println("Port: " + exposedPort); // Port: 5432
        System.out.println("Database: " + database); // Database: test
        System.out.println("Username: " + username); // Username: test
        System.out.println("Repository: " + repository); // Repository: postgres
        System.out.println("Tag: " + tag); // Tag: 17-alpine
        System.out.println("Extension: " + extension); // Extension: Testcontainers
        System.out.println("Field: " + containerAnnotation); // Field: Container
        System.out.println("Bean: " + serviceConnectionAnnotation); // Bean: ServiceConnection
        System.out.println(
                "ServiceConnection beans: " + serviceConnectionBeans); // ServiceConnection beans: 1
        System.out.println("Per-method: " + perMethodMillis + " ms"); // Per-method: 912000 ms
        System.out.println("Per-class: " + perClassMillis + " ms"); // Per-class: 114000 ms
        System.out.println("Singleton: " + singletonMillis + " ms"); // Singleton: 9500 ms
        System.out.println("Saved: " + savedMillis + " ms"); // Saved: 104500 ms
    }
}
