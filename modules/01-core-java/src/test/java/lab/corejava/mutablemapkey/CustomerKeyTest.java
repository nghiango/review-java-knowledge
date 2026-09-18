package lab.corejava.mutablemapkey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerKeyTest {

    @Test
    void equalValues_remainUsableAsMapKeys() {
        var first = new CustomerKey("tenant-a", "customer-42");
        var sameIdentity = new CustomerKey("tenant-a", "customer-42");
        var cache = new HashMap<CustomerKey, String>();

        cache.put(first, "LOW");

        assertThat(cache.get(sameIdentity)).isEqualTo("LOW");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void constructor_blankTenantId_isRejected(String tenantId) {
        assertThatThrownBy(() -> new CustomerKey(tenantId, "customer-42"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void constructor_blankCustomerId_isRejected(String customerId) {
        assertThatThrownBy(() -> new CustomerKey("tenant-a", customerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId");
    }
}
