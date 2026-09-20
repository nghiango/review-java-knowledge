package lab.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lab.testsupport.SharedKafkaContainer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.kafka.KafkaContainer;

class KafkaIntegrationTest {

    private static KafkaContainer kafkaContainer;
    private static String bootstrapServers;
    private static KafkaProducer<String, String> producer;

    @BeforeAll
    static void startKafka() {
        kafkaContainer = SharedKafkaContainer.instance();
        bootstrapServers = kafkaContainer.getBootstrapServers();

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        producer = new KafkaProducer<>(producerProps);
    }

    @AfterAll
    static void closeKafka() {
        if (producer != null) {
            producer.close(Duration.ofSeconds(5));
        }
    }

    @Test
    @DisplayName(
            "Kafka producer sends keyed messages to multi-partition topic with consistent partition routing")
    void producerSend_keyedRecords_routesToSamePartition() throws Exception {
        String topic = "test-keyed-ordering-" + System.currentTimeMillis();

        try (AdminClient admin =
                AdminClient.create(
                        Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            admin.createTopics(Collections.singletonList(new NewTopic(topic, 3, (short) 1)))
                    .all()
                    .get(10, TimeUnit.SECONDS);
        }

        RecordMetadata meta1 =
                producer.send(new ProducerRecord<>(topic, "user-42", "event-1"))
                        .get(5, TimeUnit.SECONDS);
        RecordMetadata meta2 =
                producer.send(new ProducerRecord<>(topic, "user-42", "event-2"))
                        .get(5, TimeUnit.SECONDS);
        RecordMetadata meta3 =
                producer.send(new ProducerRecord<>(topic, "user-42", "event-3"))
                        .get(5, TimeUnit.SECONDS);

        assertThat(meta1.partition()).isEqualTo(meta2.partition()).isEqualTo(meta3.partition());
        assertThat(meta2.offset()).isGreaterThan(meta1.offset());
        assertThat(meta3.offset()).isGreaterThan(meta2.offset());
    }

    @Test
    @DisplayName("Kafka consumer polls messages, processes in order, and commits offset")
    void consumerPoll_receivesAndCommitsOffset() throws Exception {
        String topic = "test-consumer-group-" + System.currentTimeMillis();
        String groupId = "test-group-" + System.currentTimeMillis();

        try (AdminClient admin =
                AdminClient.create(
                        Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {
            admin.createTopics(Collections.singletonList(new NewTopic(topic, 1, (short) 1)))
                    .all()
                    .get(10, TimeUnit.SECONDS);
        }

        producer.send(new ProducerRecord<>(topic, "order-1", "CREATED")).get(5, TimeUnit.SECONDS);

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProps.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));

            await().atMost(Duration.ofSeconds(20))
                    .untilAsserted(
                            () -> {
                                ConsumerRecords<String, String> records =
                                        consumer.poll(Duration.ofMillis(500));
                                assertThat(records).isNotEmpty();
                                ConsumerRecord<String, String> record = records.iterator().next();
                                assertThat(record.key()).isEqualTo("order-1");
                                assertThat(record.value()).isEqualTo("CREATED");
                                consumer.commitSync();
                            });
        }
    }
}
