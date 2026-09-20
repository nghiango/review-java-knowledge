package lab.performance.batchloading;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OrderSummaryLoaderTest {
    @Test
    void loadRecent_multipleOrders_fetchesCustomersOnce() {
        AtomicInteger customerQueries = new AtomicInteger();
        OrderRepository orders = limit -> List.of(new Order(1, 10), new Order(2, 20));
        CustomerRepository customers =
                ids -> {
                    customerQueries.incrementAndGet();
                    assertThat(ids).containsExactlyInAnyOrder(10L, 20L);
                    return Map.of(10L, new Customer(10, "Ada"), 20L, new Customer(20, "Linus"));
                };

        List<OrderSummary> summaries = new OrderSummaryLoader(orders, customers).loadRecent(100);

        assertThat(summaries)
                .containsExactly(new OrderSummary(1, "Ada"), new OrderSummary(2, "Linus"));
        assertThat(customerQueries).hasValue(1);
    }

    @Test
    void loadRecent_duplicateCustomer_deduplicatesBatchKeys() {
        OrderRepository orders = limit -> List.of(new Order(1, 10), new Order(2, 10));
        CustomerRepository customers = ids -> Map.of(10L, new Customer(10, onlyId(ids).toString()));

        assertThat(new OrderSummaryLoader(orders, customers).loadRecent(10))
                .extracting(OrderSummary::customerName)
                .containsOnly("10");
    }

    private static Long onlyId(Collection<Long> ids) {
        assertThat(ids).containsExactly(10L);
        return ids.iterator().next();
    }
}
