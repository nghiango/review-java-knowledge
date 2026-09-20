package lab.kafka.questions;

/** Q11: What is consumer lag, why is it a critical SLO metric, and how is it calculated? */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q11ConsumerLagMonitoring {

    public static void main(String[] args) {
        // Log End Offset (LEO): highest offset written to the broker partition
        long logEndOffset = 150000L;

        // Current Consumer Offset: highest offset committed by the consumer group
        long committedOffset = 148500L;

        // Consumer Lag = LEO - Current Offset
        long consumerLag = logEndOffset - committedOffset; // 1500L

        // Monitored via Burrow, Prometheus/Grafana, or Micrometer metric:
        // kafka.consumer.fetch.manager.records.lag
        boolean isBacklogGrowing = (consumerLag > 1000L); // true

        // Growing lag indicates consumer throughput < producer throughput or consumer stalled on
        // lock/I/O
        boolean alertsOnPersistentLagSpike = true; // true
    }
}
