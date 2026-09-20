package lab.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lab.testsupport.SharedRabbitContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;

class RabbitMqIntegrationTest {

    private static RabbitMQContainer container;
    private static Connection connection;
    private static Channel channel;

    private static final String EXCHANGE = "test.direct.exchange";
    private static final String ROUTING_KEY = "task.order";
    private static final String QUEUE = "test.work.queue";

    private static final String DLX_EXCHANGE = "test.dlx";
    private static final String DLQ_QUEUE = "test.dlq";
    private static final String DLQ_ROUTING_KEY = "deadletter";

    @BeforeAll
    static void setUp() throws Exception {
        container = SharedRabbitContainer.instance();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(container.getHost());
        factory.setPort(container.getAmqpPort());
        factory.setUsername(container.getAdminUsername());
        factory.setPassword(container.getAdminPassword());

        connection = factory.newConnection();
        channel = connection.createChannel();

        // Configure Dead Letter topology
        channel.exchangeDeclare(DLX_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.queueDeclare(DLQ_QUEUE, true, false, false, null);
        channel.queueBind(DLQ_QUEUE, DLX_EXCHANGE, DLQ_ROUTING_KEY);

        // Configure Work Queue with DLX arguments
        channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT, true);
        Map<String, Object> queueArgs = new HashMap<>();
        queueArgs.put("x-dead-letter-exchange", DLX_EXCHANGE);
        queueArgs.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        channel.queueDeclare(QUEUE, true, false, false, queueArgs);
        channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

    @Test
    @DisplayName("Publishes message with publisher confirm and consumes with manual acknowledgment")
    void publishAndConsume_withManualAck() throws Exception {
        channel.confirmSelect();

        String payload = "{\"orderId\":\"ord-999\",\"status\":\"CONFIRMED\"}";
        AMQP.BasicProperties props =
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2) // persistent
                        .build();

        channel.basicPublish(
                EXCHANGE, ROUTING_KEY, props, payload.getBytes(StandardCharsets.UTF_8));
        boolean confirmed = channel.waitForConfirms(5000);
        assertThat(confirmed).isTrue();

        // Consume single message synchronously
        GetResponse response = channel.basicGet(QUEUE, false); // manual ack
        assertThat(response).isNotNull();

        String received = new String(response.getBody(), StandardCharsets.UTF_8);
        assertThat(received).isEqualTo(payload);

        channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
    }

    @Test
    @DisplayName("Rejected message with requeue=false routes to Dead Letter Queue")
    void rejectWithoutRequeue_routesToDeadLetterQueue() throws Exception {
        String payload = "{\"orderId\":\"ord-poison\",\"error\":\"CORRUPT\"}";
        AMQP.BasicProperties props =
                new AMQP.BasicProperties.Builder()
                        .contentType("application/json")
                        .deliveryMode(2)
                        .build();

        channel.basicPublish(
                EXCHANGE, ROUTING_KEY, props, payload.getBytes(StandardCharsets.UTF_8));

        GetResponse response = channel.basicGet(QUEUE, false);
        assertThat(response).isNotNull();

        // Reject without requeue: RabbitMQ routes to test.dlx -> test.dlq
        channel.basicReject(response.getEnvelope().getDeliveryTag(), false);

        // Verify message arrived in DLQ asynchronously via Awaitility
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            GetResponse dlqResponse = channel.basicGet(DLQ_QUEUE, true);
                            assertThat(dlqResponse).isNotNull();
                            String dlqPayload =
                                    new String(dlqResponse.getBody(), StandardCharsets.UTF_8);
                            assertThat(dlqPayload).isEqualTo(payload);
                        });
    }
}
