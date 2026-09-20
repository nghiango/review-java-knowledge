package lab.springtransactions.rollbackrules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RollbackRulesTransactionTest {

    @Test
    @DisplayName("Should successfully confirm valid order")
    void placeOrder_validAmount_confirmsOrder() throws OrderValidationException {
        OrderPlacementService service = new OrderPlacementService();

        service.placeOrder("ord-301", 99.0);

        assertThat(service.getOrderStatus("ord-301")).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Should throw OrderValidationException and roll back order on invalid amount")
    void placeOrder_invalidAmount_throwsExceptionAndRollsBack() {
        OrderPlacementService service = new OrderPlacementService();

        assertThatThrownBy(() -> service.placeOrder("ord-302", -5.0))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Invalid order amount");

        assertThat(service.getOrderStatus("ord-302")).isNull();
    }
}
