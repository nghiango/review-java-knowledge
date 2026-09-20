package lab.observability.questions;

public class Q04ActuatorEndpointsBasicsExample {

    record ActuatorEndpoint(String path, String purpose, boolean sensitiveByDefault) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Spring Boot Actuator exposes operational endpoints for monitoring and administration:
        // - /actuator/health: Kubernetes readiness/liveness checks (UP/DOWN).
        // - /actuator/prometheus: Formatted scrape output for Prometheus collector.
        // - /actuator/metrics: Ad-hoc dimensional querying of registered meters.
        // - /actuator/env, /beans: Sensitive diagnostic configuration (must be secured in prod).
        ActuatorEndpoint health =
                new ActuatorEndpoint("/actuator/health", "Readiness & Liveness probes", false);
        ActuatorEndpoint prometheus =
                new ActuatorEndpoint("/actuator/prometheus", "Prometheus TSDB scraper", false);
        ActuatorEndpoint env =
                new ActuatorEndpoint("/actuator/env", "Environment & properties inspection", true);

        boolean healthPublic = !health.sensitiveByDefault(); // true
        boolean envSensitive = env.sensitiveByDefault(); // true

        System.out.println("Health endpoint is safe for unauthenticated probes: " + healthPublic);
        System.out.println("Env endpoint must be secured: " + envSensitive);
    }
}
