package lab.springboot.actuatorsecurity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;

class ActuatorSecurityConfigTest {

    @Test
    @DisplayName("Should detect insecure exposure when wildcard is configured")
    void isSafeExposureConfigured_wildcard_returnsFalse() {
        WebEndpointProperties properties = new WebEndpointProperties();
        properties.getExposure().setInclude(Set.of("*"));

        ActuatorSecurityConfig config = new ActuatorSecurityConfig(properties);

        assertThat(config.isSafeExposureConfigured()).isFalse();
    }

    @Test
    @DisplayName("Should approve exposure when only safe endpoints are configured")
    void isSafeExposureConfigured_onlySafeEndpoints_returnsTrue() {
        WebEndpointProperties properties = new WebEndpointProperties();
        properties.getExposure().setInclude(Set.of("health", "info", "metrics"));

        ActuatorSecurityConfig config = new ActuatorSecurityConfig(properties);

        assertThat(config.isSafeExposureConfigured()).isTrue();
        assertThat(config.getExposedEndpoints())
                .containsExactlyInAnyOrder("health", "info", "metrics");
    }

    @Test
    @DisplayName("Should reject exposure if dangerous env or shutdown endpoint is included")
    void isSafeExposureConfigured_sensitiveEndpoint_returnsFalse() {
        WebEndpointProperties properties = new WebEndpointProperties();
        properties.getExposure().setInclude(Set.of("health", "env", "shutdown"));

        ActuatorSecurityConfig config = new ActuatorSecurityConfig(properties);

        assertThat(config.isSafeExposureConfigured()).isFalse();
    }
}
