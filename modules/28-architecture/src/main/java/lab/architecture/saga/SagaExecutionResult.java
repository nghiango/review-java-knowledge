package lab.architecture.saga;

import java.util.Collections;
import java.util.List;

/** Encapsulates the overall result of a Saga execution. */
public record SagaExecutionResult(
        String sagaId,
        SagaState finalState,
        List<String> completedSteps,
        List<String> compensatedSteps,
        String failureReason) {
    public SagaExecutionResult {
        completedSteps = Collections.unmodifiableList(completedSteps);
        compensatedSteps = Collections.unmodifiableList(compensatedSteps);
    }

    public boolean isSuccessful() {
        return finalState == SagaState.COMPLETED;
    }
}
