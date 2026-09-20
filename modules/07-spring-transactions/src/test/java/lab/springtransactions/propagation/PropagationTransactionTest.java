package lab.springtransactions.propagation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropagationTransactionTest {

    @Test
    @DisplayName("Should create order and participate in transaction with audit logging")
    void createOrder_validOrder_recordsOrderAndAudit() {
        AuditLogService auditLogService = new AuditLogService();
        OrderService orderService = new OrderService(auditLogService);

        orderService.createOrder("ord-401", 120.0);

        assertThat(orderService.getOrderAmount("ord-401")).isEqualTo(120.0);
        assertThat(auditLogService.getLogs()).contains("ord-401:ORDER_CREATED");
    }

    @Test
    @DisplayName("Should fail audit when audit service throws exception")
    void createOrder_auditFails_propagatesException() {
        AuditLogService auditLogService = new AuditLogService();

        assertThatThrownBy(() -> auditLogService.recordAudit("ord-402", "FAIL_AUDIT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Audit destination unavailable");
    }
}
