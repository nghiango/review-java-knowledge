package lab.java25boot4.springtransactions.questions;

public class Q06HikariPoolSizingVirtualThreadsExample {

    public static void main(String[] args) {
        // While virtual threads can scale to 100,000+, the database pool cannot.
        // HikariCP pool size should remain aligned with DB core capacity (e.g. 2 * cores +
        // effective_spindle_count).
        int cpuCores = 8;
        int recommendedPoolSize = 2 * cpuCores + 1; // 17
        System.out.println(
                "Recommended HikariCP maximumPoolSize: "
                        + recommendedPoolSize); // Recommended HikariCP maximumPoolSize: 17
    }
}
