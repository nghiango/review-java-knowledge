package lab.performance.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricLineEncoderTest {
    private final MetricLineEncoder encoder = new MetricLineEncoder();

    @Test
    void encode_unsortedTags_producesDeterministicEscapedLine() {
        var event = new MetricEvent("requests", Map.of("zone", "a,b", "route", "x=y"), 7);

        assertThat(encoder.encode(event)).isEqualTo("requests{route=x\\=y,zone=a\\,b}=7");
    }

    @Test
    void encode_emptyTags_preservesWireShape() {
        assertThat(encoder.encode(new MetricEvent("jobs", Map.of(), 3))).isEqualTo("jobs{}=3");
    }

    @Test
    void constructor_mutableTags_defensivelyCopies() {
        Map<String, String> tags = new HashMap<>();
        tags.put("status", "ok");
        MetricEvent event = new MetricEvent("jobs", tags, 1);
        tags.put("status", "failed");

        assertThat(encoder.encode(event)).isEqualTo("jobs{status=ok}=1");
    }

    @Test
    void encode_nullEvent_rejected() {
        assertThatNullPointerException().isThrownBy(() -> encoder.encode(null));
    }
}
