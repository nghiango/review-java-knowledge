package lab.rabbitmq.broken.unboundedprefetch;

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

@Component
public class VideoTranscodingConsumer {

    private final HeavyVideoTranscoder transcoder;

    public VideoTranscodingConsumer(HeavyVideoTranscoder transcoder) {
        this.transcoder = transcoder;
    }

    @RabbitListener(
            queues = "video.transcoding.jobs",
            containerFactory = "unboundedPrefetchContainerFactory",
            ackMode = "MANUAL")
    public void onTranscodeJob(
            TranscodingJob job,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        // Long-running video transcoding taking 30-60 seconds per job
        transcoder.transcode(job.jobId(), job.sourceVideoUrl(), job.targetFormat());
        channel.basicAck(deliveryTag, false);
    }

    @Configuration
    public static class ContainerConfig {

        @Bean
        public SimpleRabbitListenerContainerFactory unboundedPrefetchContainerFactory(
                ConnectionFactory connectionFactory) {
            SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            // Setting prefetchCount to 0 (unbounded in AMQP protocol) or a very high value
            factory.setPrefetchCount(0);
            return factory;
        }
    }

    public interface HeavyVideoTranscoder {
        void transcode(String jobId, String url, String format);
    }
}
