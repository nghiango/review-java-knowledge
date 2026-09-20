package lab.distributeddata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lab.distributeddata.inbox.JdbcInboxRepository;
import lab.distributeddata.inbox.SafeInventoryConsumer;
import lab.distributeddata.outbox.JdbcOutboxRepository;
import lab.distributeddata.outbox.OutboxEvent;
import lab.distributeddata.outbox.OutboxPublisher;
import lab.testsupport.SharedKafkaContainer;
import lab.testsupport.SharedPostgresContainer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

class DistributedDataIntegrationTest {

    private static PostgreSQLContainer<?> postgres;
    private static KafkaContainer kafka;
    private static HikariDataSource dataSource;
    private static JdbcClient jdbcClient;
    private static KafkaTemplate<String, String> kafkaTemplate;

    @BeforeAll
    static void setupInfrastructure() {
        postgres = SharedPostgresContainer.instance();
        kafka = SharedKafkaContainer.instance();

        // Create Kafka Topic
        try (AdminClient admin =
                AdminClient.create(
                        Map.of(
                                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                                kafka.getBootstrapServers()))) {
            admin.createTopics(
                    Collections.singletonList(new NewTopic("outbox.orders.topic", 1, (short) 1)));
        }

        // Configure DataSource
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgres.getJdbcUrl());
        hikariConfig.setUsername(postgres.getUsername());
        hikariConfig.setPassword(postgres.getPassword());
        hikariConfig.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(hikariConfig);
        jdbcClient = JdbcClient.create(dataSource);

        // Create SQL Schemas
        jdbcClient
                .sql(
                        """
                CREATE TABLE IF NOT EXISTS outbox_events (
                    id VARCHAR(64) PRIMARY KEY,
                    aggregate_type VARCHAR(64) NOT NULL,
                    aggregate_id VARCHAR(64) NOT NULL,
                    event_type VARCHAR(64) NOT NULL,
                    payload TEXT NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """)
                .update();

        jdbcClient
                .sql(
                        """
                CREATE TABLE IF NOT EXISTS inbox_messages (
                    message_id VARCHAR(64) NOT NULL,
                    consumer_group VARCHAR(64) NOT NULL,
                    processed_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (message_id, consumer_group)
                )
                """)
                .update();

        // Configure KafkaTemplate
        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @AfterAll
    static void teardown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    @DisplayName(
            "End-to-end: Save outbox event to PostgreSQL, publish to Kafka, and consume idempotently with inbox deduplication")
    void endToEndOutboxAndInboxFlow() {
        JdbcOutboxRepository outboxRepository = new JdbcOutboxRepository(jdbcClient);
        JdbcInboxRepository inboxRepository = new JdbcInboxRepository(jdbcClient);
        OutboxPublisher publisher = new OutboxPublisher(outboxRepository, kafkaTemplate);

        String eventId = "evt-e2e-1";
        String orderId = "ord-999";
        OutboxEvent outboxEvent =
                new OutboxEvent(
                        eventId,
                        "Order",
                        orderId,
                        "ORDER_CREATED",
                        "{\"orderId\":\"" + orderId + "\",\"sku\":\"WIDGET-1\",\"qty\":2}",
                        "PENDING",
                        Instant.now());

        // 1. Save outbox event in PostgreSQL
        outboxRepository.save(outboxEvent);
        assertThat(outboxRepository.findById(eventId)).isPresent();
        assertThat(outboxRepository.findById(eventId).get().status()).isEqualTo("PENDING");

        // 2. Outbox publisher locks batch and sends to Kafka
        int published = publisher.publishPendingEvents(10, "outbox.orders.topic");
        assertThat(published).isEqualTo(1);
        assertThat(outboxRepository.findById(eventId).get().status()).isEqualTo("PROCESSED");

        // 3. Kafka consumer verifies message reception
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-verify-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList("outbox.orders.topic"));
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(
                            () -> {
                                ConsumerRecords<String, String> records =
                                        consumer.poll(Duration.ofMillis(200));
                                assertThat(records.count()).isGreaterThanOrEqualTo(1);
                            });
        }

        // 4. Safe inventory consumer processes event via Inbox Pattern
        SafeInventoryConsumer.InventoryRepository mockInventory =
                org.mockito.Mockito.mock(SafeInventoryConsumer.InventoryRepository.class);
        SafeInventoryConsumer safeConsumer =
                new SafeInventoryConsumer(inboxRepository, mockInventory);

        // First delivery: should be processed
        boolean firstProcessed = safeConsumer.processOrderPlaced(eventId, "WIDGET-1", 2);
        assertThat(firstProcessed).isTrue();
        org.mockito.Mockito.verify(mockInventory, org.mockito.Mockito.times(1))
                .decrementStock("WIDGET-1", 2);

        // Second duplicate redelivery: should be skipped
        boolean secondProcessed = safeConsumer.processOrderPlaced(eventId, "WIDGET-1", 2);
        assertThat(secondProcessed).isFalse();
        org.mockito.Mockito.verify(mockInventory, org.mockito.Mockito.times(1))
                .decrementStock("WIDGET-1", 2);
    }
}
