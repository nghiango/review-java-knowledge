package lab.architecture.saga;

public enum SagaState {
    NOT_STARTED,
    RUNNING,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}
