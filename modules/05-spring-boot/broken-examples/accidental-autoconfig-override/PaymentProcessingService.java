package lab.springboot.broken.autoconfigoverride;

import org.springframework.stereotype.Service;

@Service
public class PaymentProcessingService {

    private final ExternalPaymentClient client;

    public PaymentProcessingService(ExternalPaymentClient client) {
        this.client = client;
    }

    public boolean isObservabilityEnabled() {
        return client.isMetricsEnabled() && client.isTracingEnabled();
    }
}
