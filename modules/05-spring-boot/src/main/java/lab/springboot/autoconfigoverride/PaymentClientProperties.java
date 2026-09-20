package lab.springboot.autoconfigoverride;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.client")
public record PaymentClientProperties(
        String baseUrl, boolean enableMetrics, boolean enableTracing) {

    public PaymentClientProperties {
        if (baseUrl == null) {
            baseUrl = "https://api.payments.default.internal";
        }
    }
}
