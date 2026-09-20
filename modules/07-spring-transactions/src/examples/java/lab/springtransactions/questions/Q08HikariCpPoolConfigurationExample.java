package lab.springtransactions.questions;

import com.zaxxer.hikari.HikariConfig;

public class Q08HikariCpPoolConfigurationExample {

    public static void main(String[] args) {
        // HikariCP connection pool configuration formula: Pool Size = (CPU cores * 2) +
        // effective_spindle_count
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000); // 30s timeout
        config.setIdleTimeout(600000); // 10m
        config.setMaxLifetime(1800000); // 30m

        int maxPoolSize = config.getMaximumPoolSize(); // 20
        long connTimeout = config.getConnectionTimeout(); // 30000
        boolean isSized = maxPoolSize > 0; // true

        System.out.println(
                "Hikari max pool: "
                        + maxPoolSize
                        + ", timeout: "
                        + connTimeout
                        + "ms, valid: "
                        + isSized);
    }
}
