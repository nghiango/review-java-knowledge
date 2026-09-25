package lab.springboot.questions;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@SuppressWarnings("unused")
public final class Q24RelaxedBindingAndValidationExample {
    private Q24RelaxedBindingAndValidationExample() {}

    // Relaxed binding allows properties defined as kebab-case, snake_case, camelCase, or UPPER_CASE:
    // e.g. "acme.payment-service.max-retry-attempts", "ACME_PAYMENTSERVICE_MAXRETRYATTEMPTS"
    @ConfigurationProperties(prefix = "acme.payment-service")
    @Validated
    public static class PaymentServiceProperties {
        @NotBlank
        private String endpointUrl;

        @Min(1)
        @Max(10)
        private int maxRetryAttempts = 3;

        public String getEndpointUrl() { return endpointUrl; }
        public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    }

    public static void main(String[] args) {
        PaymentServiceProperties props = new PaymentServiceProperties();
        props.setEndpointUrl("https://api.payment.internal");
        props.setMaxRetryAttempts(5);

        int retries = props.getMaxRetryAttempts(); // 5
        boolean isValid = retries <= 10; // true
    }
}
