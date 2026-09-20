package lab.springboot.questions;

import java.util.Set;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;

public class Q07ActuatorEndpointsSecurityExample {

    public static void main(String[] args) {
        WebEndpointProperties properties = new WebEndpointProperties();
        // Secure exposure exposes only safe diagnostic endpoints
        properties.getExposure().setInclude(Set.of("health", "info", "metrics"));

        Set<String> exposed = properties.getExposure().getInclude();
        boolean containsEnv = exposed.contains("env"); // false (env is not exposed)
        boolean containsShutdown = exposed.contains("shutdown"); // false (shutdown is disabled)
        boolean containsHealth = exposed.contains("health"); // true

        System.out.println(
                "Actuator endpoints safely exposed: "
                        + exposed
                        + ", safe: "
                        + (!containsEnv && !containsShutdown && containsHealth));
    }
}
