package lab.concurrency.asyncpipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AsyncCustomerDashboardServiceTest {

    private ExecutorService ioExecutor;

    @BeforeEach
    void setUp() {
        ioExecutor = Executors.newFixedThreadPool(4);
    }

    @AfterEach
    void tearDown() {
        ioExecutor.shutdown();
    }

    @Test
    @DisplayName("parallel dashboard assembly aggregates prices and orders non-blockingly")
    void buildDashboardAsync_aggregatesParallelResponses() throws Exception {
        PricingClient pricingClient = new PricingClient();
        OrderHistoryClient orderHistoryClient = new OrderHistoryClient();
        AsyncCustomerDashboardService service =
                new AsyncCustomerDashboardService(pricingClient, orderHistoryClient, ioExecutor);

        List<String> items = List.of("ITEM-1", "ITEM-2", "ITEM-3");
        CompletableFuture<CustomerDashboard> future = service.buildDashboardAsync("CUST-99", items);

        CustomerDashboard dashboard = future.get(5, TimeUnit.SECONDS);

        assertThat(dashboard.customerId()).isEqualTo("CUST-99");
        assertThat(dashboard.totalOrders()).isEqualTo(7);
        assertThat(dashboard.prices()).containsKeys("ITEM-1", "ITEM-2", "ITEM-3");
        assertThat(dashboard.prices().get("ITEM-1")).isEqualTo(19.99);
    }

    @Test
    @DisplayName("downstream client failures fall back gracefully without crashing entire pipeline")
    void buildDashboardAsync_handlesPartialFailureWithFallbacks() throws Exception {
        PricingClient failingPricingClient =
                new PricingClient() {
                    @Override
                    public double fetchPrice(String itemId) {
                        if ("ITEM-FAIL".equals(itemId)) {
                            throw new RuntimeException("Pricing service 503 Unavailable");
                        }
                        return 25.00;
                    }
                };
        OrderHistoryClient failingOrderClient =
                new OrderHistoryClient() {
                    @Override
                    public int fetchOrderCount(String customerId) {
                        throw new RuntimeException("Order database connection timed out");
                    }
                };

        AsyncCustomerDashboardService service =
                new AsyncCustomerDashboardService(
                        failingPricingClient, failingOrderClient, ioExecutor);

        CompletableFuture<CustomerDashboard> future =
                service.buildDashboardAsync("CUST-ERR", List.of("ITEM-OK", "ITEM-FAIL"));

        CustomerDashboard dashboard = future.get(5, TimeUnit.SECONDS);

        assertThat(dashboard.customerId()).isEqualTo("CUST-ERR");
        assertThat(dashboard.totalOrders()).isEqualTo(0); // Graceful fallback
        assertThat(dashboard.prices().get("ITEM-OK")).isEqualTo(25.00);
        assertThat(dashboard.prices().get("ITEM-FAIL")).isEqualTo(0.0); // Fallback price
    }
}
