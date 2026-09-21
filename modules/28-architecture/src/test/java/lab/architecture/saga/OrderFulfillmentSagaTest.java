package lab.architecture.saga;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import lab.architecture.saga.OrderFulfillmentSagaOrchestrator.OrderSagaContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderFulfillmentSagaTest {

    @Test
    @DisplayName("Saga succeeds when all forward steps execute without error")
    void execute_allStepsSucceed_completesSaga() {
        SagaStep<OrderSagaContext> inventoryStep =
                new SagaStep<>() {
                    @Override
                    public String getName() {
                        return "ReserveInventory";
                    }

                    @Override
                    public boolean execute(OrderSagaContext context) {
                        context.setInventoryReserved(true);
                        return true;
                    }

                    @Override
                    public void compensate(OrderSagaContext context) {
                        context.setInventoryReserved(false);
                    }
                };

        SagaStep<OrderSagaContext> paymentStep =
                new SagaStep<>() {
                    @Override
                    public String getName() {
                        return "ProcessPayment";
                    }

                    @Override
                    public boolean execute(OrderSagaContext context) {
                        context.setPaymentCharged(true);
                        return true;
                    }

                    @Override
                    public void compensate(OrderSagaContext context) {
                        context.setPaymentCharged(false);
                    }
                };

        SagaStep<OrderSagaContext> shippingStep =
                new SagaStep<>() {
                    @Override
                    public String getName() {
                        return "DispatchShipping";
                    }

                    @Override
                    public boolean execute(OrderSagaContext context) {
                        context.setShipmentCreated(true);
                        return true;
                    }

                    @Override
                    public void compensate(OrderSagaContext context) {
                        context.setShipmentCreated(false);
                    }
                };

        OrderFulfillmentSagaOrchestrator orchestrator =
                new OrderFulfillmentSagaOrchestrator(
                        List.of(inventoryStep, paymentStep, shippingStep));

        OrderSagaContext context =
                new OrderSagaContext("saga-1", "ORD-101", "CUST-1", new BigDecimal("150.00"));
        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.finalState()).isEqualTo(SagaState.COMPLETED);
        assertThat(result.completedSteps())
                .containsExactly("ReserveInventory", "ProcessPayment", "DispatchShipping");
        assertThat(result.compensatedSteps()).isEmpty();
        assertThat(context.isInventoryReserved()).isTrue();
        assertThat(context.isPaymentCharged()).isTrue();
        assertThat(context.isShipmentCreated()).isTrue();
    }

    @Test
    @DisplayName("Saga executes compensating actions in reverse order when downstream step fails")
    void execute_stepFails_triggersReverseCompensation() {
        SagaStep<OrderSagaContext> inventoryStep =
                new SagaStep<>() {
                    @Override
                    public String getName() {
                        return "ReserveInventory";
                    }

                    @Override
                    public boolean execute(OrderSagaContext context) {
                        context.setInventoryReserved(true);
                        return true;
                    }

                    @Override
                    public void compensate(OrderSagaContext context) {
                        context.setInventoryReserved(false);
                    }
                };

        SagaStep<OrderSagaContext> paymentStep =
                new SagaStep<>() {
                    @Override
                    public String getName() {
                        return "ProcessPayment";
                    }

                    @Override
                    public boolean execute(OrderSagaContext context) {
                        // Payment declined by external gateway
                        return false;
                    }

                    @Override
                    public void compensate(OrderSagaContext context) {
                        context.setPaymentCharged(false);
                    }
                };

        SagaStep<OrderSagaContext> shippingStep =
                new SagaStep<>() {
                    @Override
                    public String getName() {
                        return "DispatchShipping";
                    }

                    @Override
                    public boolean execute(OrderSagaContext context) {
                        context.setShipmentCreated(true);
                        return true;
                    }

                    @Override
                    public void compensate(OrderSagaContext context) {
                        context.setShipmentCreated(false);
                    }
                };

        OrderFulfillmentSagaOrchestrator orchestrator =
                new OrderFulfillmentSagaOrchestrator(
                        List.of(inventoryStep, paymentStep, shippingStep));

        OrderSagaContext context =
                new OrderSagaContext("saga-2", "ORD-102", "CUST-2", new BigDecimal("299.00"));
        SagaExecutionResult result = orchestrator.execute(context);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.finalState()).isEqualTo(SagaState.COMPENSATED);
        assertThat(result.completedSteps()).containsExactly("ReserveInventory");
        assertThat(result.compensatedSteps()).containsExactly("ReserveInventory");
        assertThat(context.isInventoryReserved()).isFalse(); // Compensated back to false!
        assertThat(context.isPaymentCharged()).isFalse();
        assertThat(context.isShipmentCreated()).isFalse();
    }
}
