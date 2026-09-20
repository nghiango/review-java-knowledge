package lab.springboot.broken.actuatorsecurity;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorSecurityConfig {

    private final WebEndpointProperties webEndpointProperties;

    public ActuatorSecurityConfig(WebEndpointProperties webEndpointProperties) {
        this.webEndpointProperties = webEndpointProperties;
    }

    public boolean isAllEndpointsExposed() {
        return webEndpointProperties.getExposure().getInclude().contains("*");
    }
}
