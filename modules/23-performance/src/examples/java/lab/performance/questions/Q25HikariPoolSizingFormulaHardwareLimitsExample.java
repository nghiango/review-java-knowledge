package lab.performance.questions;

/**
 * Demonstrates the HikariCP connection pool sizing formula based on spindle/core hardware limits:
 * connections = ((cpu_core_count * 2) + effective_spindle_count).
 */
public final class Q25HikariPoolSizingFormulaHardwareLimitsExample {
    public static void main(String[] args) {
        int cpuCores = 8;
        int diskSpindles = 1; // SSD / fast NVMe counts as 1 spindle
        int recommendedPoolSize = (cpuCores * 2) + diskSpindles; // 17

        boolean isSizedToHardware = recommendedPoolSize == 17; // true
        System.out.println("Hardware-aligned pool size: " + recommendedPoolSize);
        System.out.println("Matches sizing formula: " + isSizedToHardware);
    }
}
