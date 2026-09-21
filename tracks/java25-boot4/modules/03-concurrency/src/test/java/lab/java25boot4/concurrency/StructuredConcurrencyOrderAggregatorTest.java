package lab.java25boot4.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.StructuredTaskScope.FailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StructuredConcurrencyOrderAggregatorTest {

    private final StructuredConcurrencyOrderAggregator aggregator =
            new StructuredConcurrencyOrderAggregator();

    @Test
    @DisplayName("Should aggregate order successfully when all subtasks succeed")
    void aggregateOrder_allSucceed_returnsSummary() throws Throwable {
        var summary = aggregator.aggregateOrder("ORD-101", false, false);
        assertThat(summary.orderId()).isEqualTo("ORD-101");
        assertThat(summary.inventoryStatus()).isEqualTo("IN_STOCK");
        assertThat(summary.priceCents()).isEqualTo(4999);
    }

    @Test
    @DisplayName("Should abort and propagate exception when inventory fails")
    void aggregateOrder_inventoryFails_throwsException() {
        assertThatThrownBy(() -> aggregator.aggregateOrder("ORD-102", true, false))
                .isInstanceOf(FailedException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Inventory check failed for order: ORD-102");
    }

    @Test
    @DisplayName("Should abort and propagate exception when pricing fails")
    void aggregateOrder_pricingFails_throwsException() {
        assertThatThrownBy(() -> aggregator.aggregateOrder("ORD-103", false, true))
                .isInstanceOf(FailedException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Pricing engine unavailable for order: ORD-103");
    }
}
