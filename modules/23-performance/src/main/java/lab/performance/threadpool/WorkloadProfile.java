package lab.performance.threadpool;

public record WorkloadProfile(int processors, double blockingCoefficient, int queueCapacity) {
    public WorkloadProfile {
        if (processors <= 0 || blockingCoefficient < 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("invalid workload profile");
        }
    }

    public int workerCount() {
        return Math.max(1, (int) Math.ceil(processors * (1.0 + blockingCoefficient)));
    }
}
