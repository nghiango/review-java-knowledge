package lab.java25boot4.restapi.broken.httpinterface;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/payments")
public interface PaymentGatewayClient {

    @PostExchange("/charges")
    PaymentChargeResponse charge(@RequestBody PaymentChargeRequest request);

    @GetExchange("/charges/{chargeId}")
    PaymentChargeResponse getChargeStatus(@PathVariable String chargeId);

    record PaymentChargeRequest(String orderId, long amountCents, String currency) {}

    record PaymentChargeResponse(String chargeId, String status, String transactionRef) {}
}
