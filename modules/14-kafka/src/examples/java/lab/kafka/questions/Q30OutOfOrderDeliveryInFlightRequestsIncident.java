package lab.kafka.questions;

import java.util.Map;

/**
 * Q30: Production Incident: Financial ledger out-of-order corruption caused by retries with in-flight requests > 1.
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q30OutOfOrderDeliveryInFlightRequestsIncident {

    public static void main(String[] args) {
        // Incident Scenario:
        // A payment service published sequential events for account balance:
        // Event 1: Deposit $100 (Offset expected: 10)
        // Event 2: Withdraw $50 (Offset expected: 11)
        //
        // Producer Configuration:
        // - retries = 3
        // - max.in.flight.requests.per.connection = 5
        // - enable.idempotence = false (legacy client)
        //
        // Root Cause Mechanism:
        // 1. Producer sent Batch 1 (Deposit $100) and Batch 2 (Withdraw $50) concurrently across the socket.
        // 2. Batch 1 encountered a transient network glitch on broker socket and failed.
        // 3. Batch 2 succeeded and was committed at Offset 10!
        // 4. Producer retried Batch 1. The retry succeeded and was committed at Offset 11!
        // 5. Downstream ledger processed Withdrawal BEFORE Deposit, causing a false overdraft and account freeze!

        boolean inFlightRetriesInvertOrderWithoutIdempotence = true; // true

        // Remediation:
        // 1. Enable idempotence: enable.idempotence = true (default in modern Kafka).
        //    Brokers track sequence numbers per PID. Even with max.in.flight.requests.per.connection <= 5,
        //    the broker refuses to commit Batch 2 until Batch 1 has arrived in sequence!
        // 2. If idempotence cannot be used, set max.in.flight.requests.per.connection = 1.

        Map<String, String> orderingSafety =
                Map.of(
                        "idempotence=false, in-flight=5", "Silent out-of-order writes on network retry",
                        "idempotence=true, in-flight=5", "Strict causal ordering preserved by broker sequence tracking");

        boolean brokerMaintainsSequence =
                orderingSafety.get("idempotence=true, in-flight=5").contains("Strict causal ordering"); // true
    }
}
