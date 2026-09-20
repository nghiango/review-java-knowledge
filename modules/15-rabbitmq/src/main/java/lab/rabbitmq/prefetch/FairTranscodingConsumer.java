package lab.rabbitmq.prefetch;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Production-grade consumer demonstrating bounded prefetch count and fair dispatching.
 *
 * <p>Configures {@code prefetchCount = 5}, guaranteeing that RabbitMQ never pushes more than 5
 * unacknowledged messages to a worker at any time. This protects JVM heap memory from OOM and
 * distributes work fairly across competing consumer instances.
 */
@Component
public class FairTranscodingConsumer {

    public static final String QUEUE = "video.transcoding.jobs";
    public static final int FAIR_PREFETCH_COUNT = 5;

    private final HeavyVideoTranscoder transcoder;

    public FairTranscodingConsumer(HeavyVideoTranscoder transcoder) {
        this.transcoder = transcoder;
    }

    @RabbitListener(
            queues = QUEUE,
            containerFactory = "fairDispatchContainerFactory",
            ackMode = "MANUAL")
    public void onTranscodeJob(
            TranscodingJob job, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        transcoder.transcode(job.jobId(), job.sourceVideoUrl(), job.targetFormat());
        channel.basicAck(deliveryTag, false);
    }

    @Configuration
    public static class ContainerConfig {

        @Bean
        public SimpleRabbitListenerContainerFactory fairDispatchContainerFactory(
                ConnectionFactory connectionFactory) {
            SimpleRabbitListenerContainerFactory factory =
                    new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            // Bounded prefetch ensures fair dispatch and prevents JVM heap exhaustion
            factory.setPrefetchCount(FAIR_PREFETCH_COUNT);
            return factory;
        }
    }

    public interface HeavyVideoTranscoder {
        void transcode(String jobId, String url, String format);
    }
}
