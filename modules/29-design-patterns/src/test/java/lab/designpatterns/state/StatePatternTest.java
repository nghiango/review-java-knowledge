package lab.designpatterns.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatePatternTest {

    @Test
    @DisplayName("Order transitions through valid lifecycle states: DRAFT -> PAID -> SHIPPED")
    void orderLifecycle_validTransitions_succeeds() {
        OrderContext order = new OrderContext("ORD-77");

        assertThat(order.getCurrentState().getStateName()).isEqualTo("DRAFT");

        order.pay();
        assertThat(order.getCurrentState().getStateName()).isEqualTo("PAID");

        order.ship();
        assertThat(order.getCurrentState().getStateName()).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("Invalid state transitions throw IllegalStateException and preserve current state")
    void orderLifecycle_invalidTransitions_failsFast() {
        OrderContext order = new OrderContext("ORD-88");

        // Cannot ship an unpaid draft order
        assertThatThrownBy(order::ship)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot ship an unpaid draft order");

        order.pay();

        // Cannot pay again once paid
        assertThatThrownBy(order::pay)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order is already paid");

        order.ship();

        // Cannot cancel a shipped order
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot cancel an order that has already shipped");
    }
}
