package lab.java25boot4.corejava;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SafeMetricConverterTest {

    @Test
    @DisplayName("formatMetric maps each wrapper type to its own label")
    void formatMetric_shouldClassifyWrapperTypes() {
        assertThat(SafeMetricConverter.formatMetric((byte) 5)).isEqualTo("byte: 5");
        assertThat(SafeMetricConverter.formatMetric(42)).isEqualTo("int: 42");
        assertThat(SafeMetricConverter.formatMetric(42L)).isEqualTo("long: 42");
        assertThat(SafeMetricConverter.formatMetric(4.5d)).isEqualTo("double: 4.5");
        assertThat(SafeMetricConverter.formatMetric(null)).isEqualTo("null");
    }

    @Test
    @DisplayName("toSmallInt accepts values inside byte range")
    void toSmallInt_shouldAcceptInRangeValue() {
        assertThat(SafeMetricConverter.toSmallInt(100)).isEqualTo(100);
        assertThat(SafeMetricConverter.toSmallInt((int) Byte.MIN_VALUE)).isEqualTo(Byte.MIN_VALUE);
        assertThat(SafeMetricConverter.toSmallInt((int) Byte.MAX_VALUE)).isEqualTo(Byte.MAX_VALUE);
    }

    @Test
    @DisplayName("toSmallInt fails loudly instead of truncating an out-of-range value")
    void toSmallInt_shouldRejectOutOfRangeValue() {
        assertThatThrownBy(() -> SafeMetricConverter.toSmallInt(300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds byte capacity");

        assertThatThrownBy(() -> SafeMetricConverter.toSmallInt("300"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not an Integer");
    }
}
