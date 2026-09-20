package lab.springboot.actuatorsecurity;

import java.util.Set;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorSecurityConfig {

    public static final Set<String> ALLOWED_EXPOSED_ENDPOINTS =
            Set.of("health", "info", "metrics", "prometheus");

    private final WebEndpointProperties webEndpointProperties;

    public ActuatorSecurityConfig(WebEndpointProperties webEndpointProperties) {
        this.webEndpointProperties = webEndpointProperties;
    }

    public boolean isSafeExposureConfigured() {
        Set<String> include = webEndpointProperties.getExposure().getInclude();
        if (include.contains("*")) {
            return false;
        }
        return ALLOWED_EXPOSED_ENDPOINTS.containsAll(include);
    }

    public Set<String> getExposedEndpoints() {
        return webEndpointProperties.getExposure().getInclude();
    }
}
