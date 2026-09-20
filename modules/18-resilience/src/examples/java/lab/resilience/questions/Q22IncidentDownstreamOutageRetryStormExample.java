package lab.resilience.questions;

public class Q22IncidentDownstreamOutageRetryStormExample {

    record IncidentTimeline(int initialQps, int retryMultiplier, boolean hasJitter) {
        int peakStormQps() {
            return initialQps * (1 + retryMultiplier);
        }
    }

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Normal load: 5,000 QPS. 3 immediate retries without jitter upon 503 outage.
        IncidentTimeline incident = new IncidentTimeline(5000, 3, false);
        int stormQps = incident.peakStormQps(); // 20000

        boolean serviceCrushedByStorm = stormQps >= 15000; // true
        System.out.println(
                "Downstream flooded by storm QPS: "
                        + stormQps
                        + ", Outage amplified: "
                        + serviceCrushedByStorm);
    }
}
