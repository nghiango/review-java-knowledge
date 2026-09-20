# Solution — Unbounded Prefetch Buffer Exhausting JVM Heap

## Annotated code

```java
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
            // Resource management issue: Setting prefetchCount to 0 specifies unbounded prefetch in AMQP.
            // RabbitMQ will immediately push ALL messages currently in the queue to this single consumer node.
            // For heavy tasks, buffering thousands of jobs into JVM memory triggers an OutOfMemoryError (OOM)
            // and starves other idle competing workers.
            factory.setPrefetchCount(0);
            return factory;
        }
    }

    public interface HeavyVideoTranscoder {
        void transcode(String jobId, String url, String format);
    }
}
```

## Issue list

### Resource management issue: Unbounded prefetch exhausts JVM heap and starves competing workers

- **Location:** `VideoTranscodingConsumer.java:46`
- **Description:** `factory.setPrefetchCount(0)` configures AMQP `basic.qos(0)`, which instructs the broker to disable prefetch limits.
- **Impact:** In RabbitMQ's push model, if prefetch is 0 or set excessively high, the broker flushes all unacknowledged messages across the network into the consumer's TCP socket and in-memory buffer. When a batch of 1,000 video transcoding jobs is enqueued, the first worker instance to connect absorbs all 1,000 jobs into memory, even though each job takes a minute to process. This leads to two critical failures:
  1. The single consumer node exhausts heap memory and crashes with `java.lang.OutOfMemoryError`. When it crashes, the 1,000 unacknowledged jobs are requeued simultaneously, causing a thundering herd crash on the next worker node.
  2. Peer consumer instances running on other nodes sit completely idle with 0 tasks assigned, completely breaking parallel competing consumer scaling.
- **Remediation:** Configure a small, bounded prefetch count using `factory.setPrefetchCount(N)`. For long-running, heavy workloads, set `prefetchCount = 1` or a small integer (e.g. `1`–`5`) so RabbitMQ implements fair dispatch: a worker only receives a new job when it has acknowledged the current one.
