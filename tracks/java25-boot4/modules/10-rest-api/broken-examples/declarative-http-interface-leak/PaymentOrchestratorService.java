package lab.java25boot4.restapi.broken.httpinterface;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Service
public class PaymentOrchestratorService {

    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentOrchestratorService() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.payment-gateway.internal")
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        this.paymentGatewayClient = factory.createClient(PaymentGatewayClient.class);
    }

    public String processPayment(String orderId, long amountCents, String currency) {
        try {
            var request = new PaymentGatewayClient.PaymentChargeRequest(orderId, amountCents, currency);
            var response = paymentGatewayClient.charge(request);
            return response.chargeId();
        } catch (RuntimeException ex) {
            throw new RuntimeException("Payment processing failed for order " + orderId);
        }
    }
}
