package lab.resilience.questions;

import java.util.Map;

/**
 * Q28: How is speculative request hedging implemented to cut P99 tail latency without multiplying system load?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q28SpeculativeRequestHedgingP99Example {

    public static void main(String[] args) {
        // Problem - P99 Tail Latency:
        // In distributed systems, a single slow node or GC pause causes outlier response times
        // to spike to 2,000ms while P50 remains at 20ms.

        // Speculative Hedging Strategy (Dean & Barroso - The Tail at Scale):
        // 1. Send primary request to Node A.
        // 2. Wait for P95 latency threshold (e.g. 50ms).
        // 3. If no response received after 50ms, dispatch a "hedged" secondary request to Node B.
        // 4. Accept whichever response arrives first, and immediately cancel/abort the other.
        //
        // Load Impact:
        // Only 5% of requests ever spawn a secondary request, meaning total downstream traffic
        // increases by merely 5%, while slashing P99 tail latency by up to 80%!

        int p50LatencyMs = 20;
        int p95ThresholdMs = 50;
        int simulatedPrimaryLatencyMs = 800; // Stuck behind GC pause

        boolean shouldSpawnHedgedRequest = simulatedPrimaryLatencyMs > p95ThresholdMs; // true

        Map<String, String> tradeOffs =
                Map.of(
                        "Overhead", "Marginal ~5% additional network traffic",
                        "Tail Latency", "Cuts P99/P99.9 latency down close to P95 levels");

        boolean deliversTailLatencyReduction =
                tradeOffs.get("Tail Latency").contains("Cuts P99"); // true
    }
}
