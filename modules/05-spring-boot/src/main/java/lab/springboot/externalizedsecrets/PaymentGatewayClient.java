package lab.springboot.externalizedsecrets;

import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayClient {

    private final PaymentGatewayProperties properties;

    public PaymentGatewayClient(PaymentGatewayProperties properties) {
        this.properties = properties;
    }

    public String getApiKey() {
        return properties.apiKey();
    }

    public String getEndpointUrl() {
        return properties.endpointUrl();
    }

    public PaymentGatewayProperties getProperties() {
        return properties;
    }
}
