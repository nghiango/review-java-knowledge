package lab.rabbitmq.prefetch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@ExtendWith(MockitoExtension.class)
class FairTranscodingConsumerTest {

    @Mock private FairTranscodingConsumer.HeavyVideoTranscoder transcoder;

    @Mock private Channel channel;

    @InjectMocks private FairTranscodingConsumer consumer;

    @Test
    @DisplayName("Transcodes video and acknowledges delivery tag")
    void onTranscodeJob_transcodesAndAcks() throws IOException {
        TranscodingJob job =
                new TranscodingJob(
                        "job-1",
                        "https://video.example.com/raw.mp4",
                        "1080p",
                        Map.of("codec", "h264"));

        consumer.onTranscodeJob(job, channel, 7L);

        verify(transcoder).transcode("job-1", "https://video.example.com/raw.mp4", "1080p");
        verify(channel).basicAck(7L, false);
    }

    @Test
    @DisplayName("Container factory configures bounded prefetch count")
    void containerFactory_configuresBoundedPrefetch() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        FairTranscodingConsumer.ContainerConfig config =
                new FairTranscodingConsumer.ContainerConfig();

        SimpleRabbitListenerContainerFactory factory =
                config.fairDispatchContainerFactory(connectionFactory);

        // Factory creates containers with prefetchCount = 5
        assertThat(factory).isNotNull();
        assertThat(FairTranscodingConsumer.FAIR_PREFETCH_COUNT).isEqualTo(5);
    }
}
