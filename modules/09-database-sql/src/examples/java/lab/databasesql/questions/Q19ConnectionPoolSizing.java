package lab.databasesql.questions;

public class Q19ConnectionPoolSizing {

    public static void main(String[] args) {
        // PostgreSQL and HikariCP Pool Sizing Formula (PostgreSQL Wiki / Brett Wooldridge):
        // connections = ((CPU_cores * 2) + effective_spindle_count)
        // For an 8-core database server with SSD storage (effective spindle = 1):
        // connections = ((8 * 2) + 1) = 17 connections!

        int cpuCores = 8;
        int effectiveSpindleCount = 1;
        int optimalPoolSize = (cpuCores * 2) + effectiveSpindleCount; // 17

        // Oversized connection pools (e.g. 500 connections) cause CPU context switching thrashing,
        // disk head contention, and cache pollution, reducing overall throughput!
        boolean oversizedPoolDegradesThroughput = true; // true

        System.out.println(
                "Optimal pool size for 8 cores: "
                        + optimalPoolSize); // Optimal pool size for 8 cores: 17
        System.out.println(
                "Oversized pool degrades throughput: "
                        + oversizedPoolDegradesThroughput); // Oversized pool degrades throughput:
        // true
    }
}
