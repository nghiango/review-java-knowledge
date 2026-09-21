package lab.designpatterns.chain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChainOfResponsibilityTest {

    private OrderValidationHandler validationPipeline;

    @BeforeEach
    void setUp() {
        OrderValidationHandler fraud = new FraudCheckHandler();
        OrderValidationHandler credit = new CustomerCreditCheckHandler();
        OrderValidationHandler inventory = new InventoryCheckHandler();

        fraud.setNext(credit).setNext(inventory);
        this.validationPipeline = fraud;
    }

    @Test
    @DisplayName("Valid order passes through all pipeline handlers without errors")
    void handle_validOrder_succeeds() {
        OrderValidationContext context =
                new OrderValidationContext(
                        "ORD-VALID-1", "CUST-VERIFIED-1", new BigDecimal("500.00"));

        validationPipeline.handle(context);

        assertThat(context.isValid()).isTrue();
        assertThat(context.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Fraudulent order is blocked immediately at first handler in chain")
    void handle_fraudulentOrder_shortCircuits() {
        OrderValidationContext context =
                new OrderValidationContext("ORD-FRAUD-9", "CUST-1", new BigDecimal("100.00"));

        validationPipeline.handle(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getErrors())
                .containsExactly("Order blocked by automated fraud heuristic");
    }

    @Test
    @DisplayName("Unverified customer exceeding credit limit is flagged by second handler")
    void handle_unverifiedExcessCredit_flagged() {
        OrderValidationContext context =
                new OrderValidationContext(
                        "ORD-2", "UNVERIFIED-CUST-99", new BigDecimal("2500.00"));

        validationPipeline.handle(context);

        assertThat(context.isValid()).isFalse();
        assertThat(context.getErrors())
                .containsExactly("Order exceeds maximum credit limit for unverified customer");
    }
}
