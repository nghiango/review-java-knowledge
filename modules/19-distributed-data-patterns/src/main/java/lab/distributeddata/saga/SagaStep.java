package lab.distributeddata.saga;

public interface SagaStep {
    String getName();

    boolean execute();

    void compensate();
}
