package lab.springtransactions.questions;

import com.zaxxer.hikari.HikariConfig;

public class Q15HikariConnectionLeakDetectionExample {

    public static void main(String[] args) {
        // HikariCP leakDetectionThreshold logs stack traces of connections held longer than the
        // threshold
        HikariConfig config = new HikariConfig();
        config.setLeakDetectionThreshold(5000); // 5 seconds

        long threshold = config.getLeakDetectionThreshold(); // 5000
        boolean isLeakDetectionActive = threshold > 0; // true

        System.out.println(
                "Leak detection threshold: " + threshold + "ms, active: " + isLeakDetectionActive);
    }
}
