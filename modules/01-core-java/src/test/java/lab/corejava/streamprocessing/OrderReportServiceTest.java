package lab.corejava.streamprocessing;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OrderReportServiceTest {

    @Test
    void generate_ordersArePricedWithoutMutatingInputAndOutputIsImmutable() {
        var orders =
                List.of(new Order("order-2", "customer-b"), new Order("order-1", "customer-a"));
        PriceClient client =
                id ->
                        new OrderPrice(
                                new BigDecimal(id.endsWith("1") ? "10.00" : "20.00"),
                                Currency.getInstance("USD"));
        var service = new OrderReportService(client, Runnable::run);

        var result = service.generate(orders);

        assertThat(result)
                .extracting(priced -> priced.order().orderId())
                .containsExactly("order-2", "order-1");
        assertThat(orders).extracting(Order::orderId).containsExactly("order-2", "order-1");
        assertThatThrownBy(() -> result.clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void generate_lookupFailure_retainsOrderContextAndCause() {
        var failure = new IllegalStateException("pricing unavailable");
        var service =
                new OrderReportService(
                        id -> {
                            throw failure;
                        },
                        Runnable::run);

        assertThatThrownBy(() -> service.generate(List.of(new Order("order-9", "customer-a"))))
                .isInstanceOf(OrderPricingException.class)
                .hasMessageContaining("order-9")
                .hasCause(failure)
                .extracting("orderId")
                .isEqualTo("order-9");
    }

    @Test
    void generateConcurrently_executorBoundsWorkAndInputOrderIsPreserved() throws Exception {
        var active = new AtomicInteger();
        var maximumActive = new AtomicInteger();
        var twoCallsEntered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        PriceClient client =
                id -> {
                    var current = active.incrementAndGet();
                    maximumActive.accumulateAndGet(current, Math::max);
                    twoCallsEntered.countDown();
                    try {
                        if (!release.await(5, SECONDS))
                            throw new IllegalStateException("test release timed out");
                        return new OrderPrice(BigDecimal.ONE, Currency.getInstance("USD"));
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    } finally {
                        active.decrementAndGet();
                    }
                };

        try (var pricingPool = Executors.newFixedThreadPool(2)) {
            var service = new OrderReportService(client, pricingPool);
            var orders =
                    List.of(new Order("one", "c"), new Order("two", "c"), new Order("three", "c"));
            var invocation =
                    CompletableFuture.supplyAsync(() -> service.generateConcurrently(orders));

            assertThat(twoCallsEntered.await(5, SECONDS)).isTrue();
            assertThat(maximumActive).hasValue(2);
            release.countDown();

            assertThat(invocation.get(5, SECONDS))
                    .extracting(priced -> priced.order().orderId())
                    .containsExactly("one", "two", "three");
        }
    }
}
