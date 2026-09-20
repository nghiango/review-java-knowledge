package lab.distributeddata.saga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SagaCoordinator {

    public enum SagaStatus {
        SUCCESS,
        COMPENSATED,
        FAILED
    }

    public record SagaExecutionResult(
            SagaStatus status, String failedStep, List<String> executedCompensations) {}

    public SagaExecutionResult executeSaga(List<SagaStep> steps) {
        List<SagaStep> completedSteps = new ArrayList<>();

        for (SagaStep step : steps) {
            try {
                boolean stepSuccess = step.execute();
                if (!stepSuccess) {
                    List<String> compensations = rollback(completedSteps);
                    return new SagaExecutionResult(
                            SagaStatus.COMPENSATED, step.getName(), compensations);
                }
                completedSteps.add(step);
            } catch (Exception ex) {
                List<String> compensations = rollback(completedSteps);
                return new SagaExecutionResult(
                        SagaStatus.COMPENSATED, step.getName(), compensations);
            }
        }

        return new SagaExecutionResult(SagaStatus.SUCCESS, null, Collections.emptyList());
    }

    private List<String> rollback(List<SagaStep> completedSteps) {
        List<String> compensatedStepNames = new ArrayList<>();
        // Compensate in reverse order
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            step.compensate();
            compensatedStepNames.add(step.getName());
        }
        return compensatedStepNames;
    }
}
