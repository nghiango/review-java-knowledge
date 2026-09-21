package lab.architecture.saga;

/**
 * An individual step within a distributed Saga workflow. Encapsulates the forward execution and its
 * corresponding compensatory rollback action.
 *
 * @param <T> the saga workflow context type
 */
public interface SagaStep<T> {

    /** Name identifier for this saga step. */
    String getName();

    /**
     * Executes the forward business operation.
     *
     * @param context the mutable saga context
     * @return true if the step succeeded, false if it failed
     */
    boolean execute(T context);

    /**
     * Executes the compensating rollback action to undo the effects of execute(). Must be
     * idempotent and safe to retry.
     *
     * @param context the mutable saga context
     */
    void compensate(T context);
}
