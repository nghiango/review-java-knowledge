package lab.distributeddata.questions;

public class Q19SagaFailureRecoveryDuringCoordinatorRestartExample {

    record SagaLogRecord(String sagaId, String currentStep, String state, boolean isResumable) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // If the Saga coordinator crashes mid-flight, it must persist saga state to a database
        // (Saga Execution Log) so a newly elected coordinator can resume execution or execute
        // compensations.
        SagaLogRecord persistedSaga =
                new SagaLogRecord("saga-99", "ReserveCredit", "STARTED", true);

        boolean canResumeAfterCrash = persistedSaga.isResumable(); // true
        System.out.println(
                "Saga state recovered from persistent log upon coordinator reboot: "
                        + canResumeAfterCrash);
    }
}
