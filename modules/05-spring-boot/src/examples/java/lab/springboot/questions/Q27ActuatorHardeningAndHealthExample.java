package lab.springboot.questions;

import java.util.Locale;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

@SuppressWarnings("unused")
public final class Q27ActuatorHardeningAndHealthExample {
    private Q27ActuatorHardeningAndHealthExample() {}

    // Custom Health Indicator: aggregates internal downstream service status
    public static class PaymentGatewayHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            boolean gatewayReachable = true;
            if (gatewayReachable) {
                return Health.up()
                    .withDetail("gateway", "payment-internal-service")
                    .withDetail("latencyMs", 12)
                    .build();
            }
            return Health.down().withDetail("reason", "Gateway timeout").build();
        }
    }

    // SanitizingFunction: hides sensitive secrets/tokens in env and configprops Actuator endpoints
    public static class CustomSecretSanitizer implements SanitizingFunction {
        @Override
        public SanitizableData apply(SanitizableData data) {
            String key = data.getKey().toLowerCase(Locale.ROOT);
            if (key.contains("apikey") || key.contains("secret")) {
                return data.withSanitizedValue(); // Replaces raw secret with "******"
            }
            return data;
        }
    }

    public static void main(String[] args) {
        HealthIndicator indicator = new PaymentGatewayHealthIndicator();
        Health status = indicator.health();
        String code = status.getStatus().getCode(); // "UP"

        SanitizingFunction sanitizer = new CustomSecretSanitizer();
        SanitizableData secretData = new SanitizableData(null, "payment.apiKey", "super-secret-123");
        SanitizableData sanitized = sanitizer.apply(secretData);
        Object sanitizedValue = sanitized.getValue(); // "******"
    }
}
