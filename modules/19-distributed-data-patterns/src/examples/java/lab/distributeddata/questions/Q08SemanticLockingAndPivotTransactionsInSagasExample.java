package lab.distributeddata.questions;

public class Q08SemanticLockingAndPivotTransactionsInSagasExample {

    enum StepType {
        COMPENSATABLE,
        PIVOT,
        RETRYABLE
    }

    record SagaStepDefinition(String name, StepType type) {
        boolean canBeCompensated() {
            return type == StepType.COMPENSATABLE;
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Pivot transaction: The point of no return. Once the Pivot succeeds, the Saga CANNOT
        // abort;
        // all subsequent steps must be retryable forward to completion.
        SagaStepDefinition reserveCredit =
                new SagaStepDefinition("ReserveCredit", StepType.COMPENSATABLE);
        SagaStepDefinition chargeCard = new SagaStepDefinition("ChargeCard", StepType.PIVOT);
        SagaStepDefinition sendReceipt = new SagaStepDefinition("SendReceipt", StepType.RETRYABLE);

        boolean creditCanCompensate = reserveCredit.canBeCompensated(); // true
        boolean chargeCannotCompensate = !chargeCard.canBeCompensated(); // true

        System.out.println(
                "Compensatable step: "
                        + creditCanCompensate
                        + ", Pivot step non-abortable: "
                        + chargeCannotCompensate);
    }
}
