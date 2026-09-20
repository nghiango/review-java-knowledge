package lab.performance.connectionpool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConnectionPoolBudgetTest {
    @Test
    void maximumPoolSize_databaseBudget_sharedAcrossInstances() {
        assertThat(new ConnectionPoolBudget(100, 10, 6).maximumPoolSize()).isEqualTo(15);
    }

    @Test
    void constructor_reservedConnectionsConsumeBudget_rejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ConnectionPoolBudget(10, 10, 1));
    }

    @Test
    void create_validBudget_configuresBoundedFailFastPool() {
        var config =
                new PoolConfiguration()
                        .create(
                                "jdbc:postgresql://localhost/orders",
                                new ConnectionPoolBudget(50, 10, 4),
                                Duration.ofMillis(750));

        assertThat(config.getMaximumPoolSize()).isEqualTo(10);
        assertThat(config.getMinimumIdle()).isZero();
        assertThat(config.getConnectionTimeout()).isEqualTo(750);
    }
}
