package lab.testsupport;

import org.testcontainers.kafka.KafkaContainer;

/**
 * A single Kafka container shared by every integration test in the JVM.
 *
 * <p>Starting a Kafka container per test class is slow, so integration tests share one started
 * instance instead. The container is started lazily on the first call to {@link #instance()} and
 * stopped by Testcontainers' Ryuk reaper when the JVM exits.
 *
 * <p>Uses {@code apache/kafka-native:3.8.0} for fast startup times.
 */
public final class SharedKafkaContainer {

    private static final String IMAGE = "apache/kafka-native:3.8.0";

    private static KafkaContainer instance;

    private SharedKafkaContainer() {}

    /**
     * Returns the shared, started Kafka container, starting it on first use.
     *
     * <p>Synchronised so that parallel test execution cannot start two containers or observe a
     * half-started one.
     */
    public static synchronized KafkaContainer instance() {
        if (instance == null) {
            @SuppressWarnings("resource")
            KafkaContainer container = new KafkaContainer(IMAGE);
            container.start();
            instance = container;
        }
        return instance;
    }
}
