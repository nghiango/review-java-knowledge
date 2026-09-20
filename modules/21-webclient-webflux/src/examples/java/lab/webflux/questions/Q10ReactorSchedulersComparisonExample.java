package lab.webflux.questions;

public class Q10ReactorSchedulersComparisonExample {

    record SchedulerProfile(String name, String intendedWorkload, boolean isBounded) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Reactor Schedulers:
        // - Schedulers.parallel(): Fixed pool sized to available CPU cores; for CPU-intensive
        // non-blocking computation.
        // - Schedulers.boundedElastic(): Elastic pool (default 10x CPU cores, max 100,000 tasks
        // queued); for blocking I/O (JDBC, legacy clients).
        // - Schedulers.single(): Single dedicated reusable thread; for periodic or serialized
        // tasks.
        // - Schedulers.immediate(): Runs immediately on the calling thread without context switch.
        SchedulerProfile parallel = new SchedulerProfile("parallel", "CPU-bound algorithms", true);
        SchedulerProfile boundedElastic =
                new SchedulerProfile("boundedElastic", "Blocking I/O / JDBC", true);
        SchedulerProfile single =
                new SchedulerProfile("single", "Serialized single-thread tasks", true);

        boolean boundedElasticDesignedForBlocking =
                boundedElastic.intendedWorkload().contains("Blocking"); // true
        boolean parallelIsFixedCpuSize = parallel.isBounded(); // true

        System.out.println(
                "Schedulers.boundedElastic designed for blocking I/O: "
                        + boundedElasticDesignedForBlocking);
    }
}
