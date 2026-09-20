package lab.springcore.circular;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

class CircularDependencyDecouplingTest {

    @Configuration
    static class TestConfig {
        // Simple test configuration registering both services
    }

    @Test
    @DisplayName(
            "event-driven architecture resolves circular dependency and processes payments cleanly")
    void eventDrivenOrderBilling_completesWithoutCycle() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext()) {
            context.register(OrderService.class, BillingService.class);
            context.refresh();

            OrderService orderService = context.getBean(OrderService.class);
            orderService.createOrder("ORD-1001", 150.0);

            assertThat(orderService.getOrderStatus("ORD-1001")).isEqualTo("PAID");
        }
    }
}
