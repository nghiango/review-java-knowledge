package lab.observability.questions;

public class Q18HeadVsTailSamplingStrategiesExample {

    record SamplingDecision(
            String type, double sampleRate, boolean retainsErrors, String decisionPoint) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Head-based vs Tail-based sampling in distributed tracing:
        // - Head-based: Sampling decision made at the ingress root span (e.g. 5% random). Low
        // memory; drops rare errors.
        // - Tail-based: All spans collected in-memory by OpenTelemetry Collector collector until
        // trace ends.
        //   Retains 100% of error traces and high-latency p99 spans, but samples down happy-path
        // 200 OK traffic.
        SamplingDecision head =
                new SamplingDecision("Head-based", 0.05, false, "Ingress edge proxy");
        SamplingDecision tail =
                new SamplingDecision("Tail-based", 1.0, true, "OTel Collector buffer");

        boolean tailGuaranteesErrorCapture = tail.retainsErrors(); // true
        boolean headIsLowMemory = !head.retainsErrors(); // true

        System.out.println(
                "Tail-based sampling guarantees error capture: " + tailGuaranteesErrorCapture);
        System.out.println("Head-based sampling is cheap at ingress: " + headIsLowMemory);
    }
}
