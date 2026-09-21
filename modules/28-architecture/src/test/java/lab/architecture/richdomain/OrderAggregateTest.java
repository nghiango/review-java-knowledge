package lab.architecture.richdomain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderAggregateTest {

    @Test
    @DisplayName("addOrUpdateItem aggregates quantity when adding existing productId")
    void addOrUpdateItem_aggregatesQuantityForSameProduct() {
        Order order = new Order(OrderId.generate(), "cust-1");

        order.addOrUpdateItem("SKU-1", Quantity.of(2), Money.of(25.00));
        order.addOrUpdateItem("SKU-1", Quantity.of(3), Money.of(25.00));

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().get(0).quantity().value()).isEqualTo(5);
        assertThat(order.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    @DisplayName("submit transitions status to SUBMITTED if valid items exist")
    void submit_validDraft_transitionsToSubmitted() {
        Order order = new Order(OrderId.generate(), "cust-1");
        order.addOrUpdateItem("SKU-1", Quantity.of(1), Money.of(10.00));

        order.submit();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SUBMITTED);
    }

    @Test
    @DisplayName("submit throws IllegalStateException if order has no items")
    void submit_emptyOrder_throwsException() {
        Order order = new Order(OrderId.generate(), "cust-1");

        assertThatThrownBy(order::submit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot submit an empty order");
    }

    @Test
    @DisplayName("getItems returns unmodifiable view preventing external tampering")
    void getItems_returnsUnmodifiableList() {
        Order order = new Order(OrderId.generate(), "cust-1");
        order.addOrUpdateItem("SKU-1", Quantity.of(1), Money.of(10.00));

        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("cancel on cancelled order throws IllegalStateException")
    void cancel_alreadyCancelled_throwsException() {
        Order order = new Order(OrderId.generate(), "cust-1");
        order.cancel("Customer request");

        assertThatThrownBy(() -> order.cancel("Duplicate"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order is already cancelled");
    }
}
