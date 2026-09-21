package lab.java25boot4.concurrency.questions;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

public class Q13ScenarioDiagnosingCarrierThreadStarvationExample {

    public static void main(String[] args) {
        // Incident scenario: 50,000 requests fan out.
        // If unmanaged legacy thread pools or unbounded blocking native operations saturate the
        // carrier pool,
        // response latencies spike exponentially.
        try (var scope = StructuredTaskScope.open()) {
            var task =
                    scope.fork(
                            () -> {
                                Thread.sleep(Duration.ofMillis(10));
                                return "Healthy Execution";
                            });

            scope.join();
            System.out.println("Result: " + task.get()); // Result: Healthy Execution
        } catch (Exception e) {
            System.out.println(
                    "Mitigation: Replace unmanaged thread pools and isolate blocking JNI native calls");
        }
    }
}
