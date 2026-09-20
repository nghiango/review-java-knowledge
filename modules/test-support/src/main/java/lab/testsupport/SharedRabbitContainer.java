package lab.testsupport;

import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A single RabbitMQ container shared by every integration test in the JVM.
 *
 * <p>Starting a container per test class is slow, so integration tests share one started instance
 * instead. The container is started lazily on the first call to {@link #instance()} and stopped by
 * Testcontainers' Ryuk reaper when the JVM exits.
 *
 * <p>Uses {@code rabbitmq:3.13-alpine}.
 */
public final class SharedRabbitContainer {

    private static final String IMAGE = "rabbitmq:3.13-alpine";

    private static RabbitMQContainer instance;

    private SharedRabbitContainer() {}

    /**
     * Returns the shared, started RabbitMQ container, starting it on first use.
     *
     * <p>Synchronised so that parallel test execution cannot start two containers or observe a
     * half-started one.
     */
    public static synchronized RabbitMQContainer instance() {
        if (instance == null) {
            @SuppressWarnings("resource")
            RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse(IMAGE));
            container.start();
            instance = container;
        }
        return instance;
    }
}
