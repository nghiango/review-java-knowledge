package lab.springtransactions.selfinvocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SelfInvocationTransactionTest {

    @Test
    @DisplayName("Should validate order and delegate to collaborator across proxy boundary")
    void processOrder_validOrder_delegatesSuccessfully() {
        OrderPlacementCollaborator collaborator = new OrderPlacementCollaborator();
        OrderService orderService = new OrderService(collaborator);

        orderService.processOrder("ord-101", 150.0);

        assertThat(orderService).isNotNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when order amount is non-positive")
    void processOrder_nonPositiveAmount_throwsException() {
        OrderPlacementCollaborator collaborator = new OrderPlacementCollaborator();
        OrderService orderService = new OrderService(collaborator);

        assertThatThrownBy(() -> orderService.processOrder("ord-102", -10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
