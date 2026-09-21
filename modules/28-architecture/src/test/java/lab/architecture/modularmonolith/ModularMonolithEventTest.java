package lab.architecture.modularmonolith;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import lab.architecture.modularmonolith.billing.BillingService;
import lab.architecture.modularmonolith.inventory.internal.InventoryServiceImpl;
import lab.architecture.modularmonolith.ordering.OrderingService;
import lab.architecture.modularmonolith.shipping.ShippingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

class ModularMonolithEventTest {

    @Test
    @DisplayName("Ordering, Billing, and Shipping communicate via events across module boundaries")
    void decoupledModules_communicateViaEvents() {
        GenericApplicationContext context = new GenericApplicationContext();

        // Register components from different bounded contexts
        context.registerBean(InventoryServiceImpl.class);
        context.registerBean(ShippingService.class);
        context.registerBean(BillingService.class);
        context.registerBean(OrderingService.class);
        context.refresh();

        OrderingService orderingService = context.getBean(OrderingService.class);
        ShippingService shippingService = context.getBean(ShippingService.class);

        // Act: Place order in ordering bounded context
        orderingService.placeOrder("ORD-999", "cust-42", new BigDecimal("89.95"));

        // Assert: Shipping bounded context reacted to the cascaded events without direct DB coupling
        String status = shippingService.getShipmentStatus("ORD-999");
        assertThat(status).isEqualTo("PREPARING_DISPATCH");

        context.close();
    }
}
