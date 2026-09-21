package lab.architecture.saga;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrator coordinating the distributed Order Fulfillment Saga. Manages the sequential forward
 * execution of saga steps and executes compensating rollback transactions in reverse order upon
 * step failure.
 */
public class OrderFulfillmentSagaOrchestrator {

    private static final Logger log =
            LoggerFactory.getLogger(OrderFulfillmentSagaOrchestrator.class);

    private final List<SagaStep<OrderSagaContext>> steps;

    public OrderFulfillmentSagaOrchestrator(List<SagaStep<OrderSagaContext>> steps) {
        this.steps = List.copyOf(Objects.requireNonNull(steps, "Steps must not be null"));
    }

    public SagaExecutionResult execute(OrderSagaContext context) {
        log.info("Starting Saga [{}] for Order [{}]", context.sagaId(), context.orderId());

        List<SagaStep<OrderSagaContext>> executedSteps = new ArrayList<>();
        List<String> completedStepNames = new ArrayList<>();
        List<String> compensatedStepNames = new ArrayList<>();

        for (SagaStep<OrderSagaContext> step : steps) {
            log.info("Executing step [{}] in Saga [{}]", step.getName(), context.sagaId());
            boolean success;
            try {
                success = step.execute(context);
            } catch (Exception e) {
                log.error(
                        "Step [{}] threw unexpected exception: {}",
                        step.getName(),
                        e.getMessage(),
                        e);
                success = false;
            }

            if (success) {
                executedSteps.add(step);
                completedStepNames.add(step.getName());
            } else {
                log.warn(
                        "Step [{}] failed in Saga [{}]. Initiating compensation rollback...",
                        step.getName(),
                        context.sagaId());
                String failureReason = "Step failed: " + step.getName();

                // Rollback previously executed steps in reverse order (LIFO)
                Collections.reverse(executedSteps);
                for (SagaStep<OrderSagaContext> executedStep : executedSteps) {
                    try {
                        log.info(
                                "Compensating step [{}] in Saga [{}]",
                                executedStep.getName(),
                                context.sagaId());
                        executedStep.compensate(context);
                        compensatedStepNames.add(executedStep.getName());
                    } catch (Exception ce) {
                        log.error(
                                "Compensation failed for step [{}]: {}",
                                executedStep.getName(),
                                ce.getMessage(),
                                ce);
                        return new SagaExecutionResult(
                                context.sagaId(),
                                SagaState.FAILED,
                                completedStepNames,
                                compensatedStepNames,
                                "Critical compensation failure on step: " + executedStep.getName());
                    }
                }

                return new SagaExecutionResult(
                        context.sagaId(),
                        SagaState.COMPENSATED,
                        completedStepNames,
                        compensatedStepNames,
                        failureReason);
            }
        }

        log.info(
                "Saga [{}] completed successfully for Order [{}]",
                context.sagaId(),
                context.orderId());
        return new SagaExecutionResult(
                context.sagaId(),
                SagaState.COMPLETED,
                completedStepNames,
                compensatedStepNames,
                null);
    }

    /** Shared context carried through the Saga execution. */
    public static class OrderSagaContext {
        private final String sagaId;
        private final String orderId;
        private final String customerId;
        private final BigDecimal amount;

        private boolean inventoryReserved;
        private boolean paymentCharged;
        private boolean shipmentCreated;

        public OrderSagaContext(
                String sagaId, String orderId, String customerId, BigDecimal amount) {
            this.sagaId = Objects.requireNonNull(sagaId);
            this.orderId = Objects.requireNonNull(orderId);
            this.customerId = Objects.requireNonNull(customerId);
            this.amount = Objects.requireNonNull(amount);
        }

        public String sagaId() {
            return sagaId;
        }

        public String orderId() {
            return orderId;
        }

        public String customerId() {
            return customerId;
        }

        public BigDecimal amount() {
            return amount;
        }

        public boolean isInventoryReserved() {
            return inventoryReserved;
        }

        public void setInventoryReserved(boolean inventoryReserved) {
            this.inventoryReserved = inventoryReserved;
        }

        public boolean isPaymentCharged() {
            return paymentCharged;
        }

        public void setPaymentCharged(boolean paymentCharged) {
            this.paymentCharged = paymentCharged;
        }

        public boolean isShipmentCreated() {
            return shipmentCreated;
        }

        public void setShipmentCreated(boolean shipmentCreated) {
            this.shipmentCreated = shipmentCreated;
        }
    }
}
