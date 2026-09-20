package lab.performance.connectionpool;

import com.zaxxer.hikari.HikariConfig;
import java.time.Duration;
import java.util.Objects;

public final class PoolConfiguration {
    public HikariConfig create(
            String jdbcUrl, ConnectionPoolBudget budget, Duration connectionTimeout) {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(connectionTimeout, "connectionTimeout");
        if (connectionTimeout.isNegative() || connectionTimeout.isZero()) {
            throw new IllegalArgumentException("connectionTimeout must be positive");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(budget.maximumPoolSize());
        config.setMinimumIdle(0);
        config.setConnectionTimeout(connectionTimeout.toMillis());
        return config;
    }
}
