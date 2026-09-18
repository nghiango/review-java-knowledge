package lab.jvm.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MetricLineEncoderTest {

    @Test
    @DisplayName("encodes metric event without tags into name=value")
    void encode_withoutTags_formatsNameAndValue() {
        var event = new MetricEvent("http_requests_total", Map.of(), 100L);
        assertThat(MetricLineEncoder.encode(event)).isEqualTo("http_requests_total=100");
    }

    @Test
    @DisplayName("encodes metric event with single tag")
    void encode_withSingleTag_formatsTag() {
        var event = new MetricEvent("cpu_usage", Map.of("core", "0"), 75L);
        assertThat(MetricLineEncoder.encode(event)).isEqualTo("cpu_usage{core=0}=75");
    }

    @Test
    @DisplayName("encodes multiple tags in deterministic sorted key order")
    void encode_withMultipleTags_sortsKeysDeterministically() {
        var tags = Map.of("status", "200", "method", "POST", "uri", "/api/v1/orders");
        var event = new MetricEvent("http_requests", tags, 42L);
        assertThat(MetricLineEncoder.encode(event))
                .isEqualTo("http_requests{method=POST,status=200,uri=/api/v1/orders}=42");
    }

    @Test
    @DisplayName("escapes backslash, comma, and equals sign in tag keys and values")
    void encode_withSpecialCharacters_escapesCorrectly() {
        var tags = Map.of("tag=key", "val,ue", "path\\name", "a=b,c\\d");
        var event = new MetricEvent("custom_metric", tags, 1L);
        assertThat(MetricLineEncoder.encode(event))
                .isEqualTo("custom_metric{path\\\\name=a\\=b\\,c\\\\d,tag\\=key=val\\,ue}=1");
    }

    @Test
    @DisplayName("rejects null event")
    void encode_nullEvent_throwsNullPointerException() {
        assertThatThrownBy(() -> MetricLineEncoder.encode(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("event must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123invalid", "metric-name", "metric name"})
    @DisplayName("rejects invalid metric names")
    void metricEvent_invalidName_throwsException(String invalidName) {
        assertThatThrownBy(() -> new MetricEvent(invalidName, Map.of(), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects null name")
    void metricEvent_nullName_throwsNullPointerException() {
        assertThatThrownBy(() -> new MetricEvent(null, Map.of(), 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name must not be null");
    }

    @Test
    @DisplayName("rejects null tags map")
    void metricEvent_nullTags_throwsNullPointerException() {
        assertThatThrownBy(() -> new MetricEvent("valid_metric", null, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tags must not be null");
    }

    @Test
    @DisplayName("rejects null or blank tag keys and values")
    void metricEvent_nullOrBlankTagContent_throwsException() {
        Map<String, String> mapWithBlankKey = new HashMap<>();
        mapWithBlankKey.put("  ", "value");
        assertThatThrownBy(() -> new MetricEvent("valid_metric", mapWithBlankKey, 1L))
                .isInstanceOf(IllegalArgumentException.class);

        Map<String, String> mapWithNullValue = new HashMap<>();
        mapWithNullValue.put("key", null);
        assertThatThrownBy(() -> new MetricEvent("valid_metric", mapWithNullValue, 1L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("defensively copies tags to guarantee event immutability")
    void metricEvent_defensiveCopy_guaranteesImmutability() {
        Map<String, String> mutableTags = new HashMap<>();
        mutableTags.put("env", "prod");
        var event = new MetricEvent("active_users", mutableTags, 500L);

        mutableTags.put("env", "staging");
        mutableTags.put("region", "us-east");

        assertThat(event.tags()).containsExactly(Map.entry("env", "prod"));
        assertThatThrownBy(() -> event.tags().put("new", "tag"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
