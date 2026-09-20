package lab.observability.questions;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

public class Q09MicrometerObservationApiExample {

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Micrometer Observation API unifies metrics and tracing under a single lifecycle:
        // A single Observation.createNotStarted("order.checkout", registry) invocation
        // automatically creates both a Micrometer Timer metric AND an OpenTelemetry / Brave
        // distributed tracing Span.
        ObservationRegistry registry = ObservationRegistry.create();

        String result =
                Observation.createNotStarted("order.checkout", registry)
                        .lowCardinalityKeyValue("payment.method", "CARD")
                        .highCardinalityKeyValue("order.id", "ord-1234")
                        .observe(
                                () -> {
                                    return "CHECKOUT_PROCESSED";
                                });

        boolean succeeded = result.equals("CHECKOUT_PROCESSED"); // true
        System.out.println("Observation execution result: " + result);
    }
}
