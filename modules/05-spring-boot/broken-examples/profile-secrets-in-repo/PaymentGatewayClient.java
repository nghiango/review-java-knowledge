package lab.springboot.broken.externalizedsecrets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayClient {

    private final String apiKey;
    private final String endpointUrl;

    public PaymentGatewayClient(
            @Value("${payment.gateway.api-key}") String apiKey,
            @Value("${payment.gateway.endpoint-url}") String endpointUrl) {
        this.apiKey = apiKey;
        this.endpointUrl = endpointUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
