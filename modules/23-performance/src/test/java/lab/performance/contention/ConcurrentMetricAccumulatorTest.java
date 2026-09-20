package lab.performance.contention;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConcurrentMetricAccumulatorTest {
    @Test
    void add_concurrentWriters_retainsEveryIncrement() throws Exception {
        var accumulator = new ConcurrentMetricAccumulator();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int index = 0; index < 10_000; index++) {
                executor.execute(() -> accumulator.add("requests", 1));
            }
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(accumulator.total("requests")).isEqualTo(10_000);
    }
}
