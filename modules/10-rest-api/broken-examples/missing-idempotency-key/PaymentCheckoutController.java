package lab.restapi.broken.idempotency;

import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentCheckoutController {

    private final PaymentService paymentService;

    public PaymentCheckoutController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record ChargeRequest(String customerId, BigDecimal amount) {}

    // POST endpoint executing non-idempotent charge without Idempotency-Key validation
    @PostMapping("/charge")
    public ResponseEntity<PaymentService.PaymentResult> chargeCard(
            @RequestBody ChargeRequest request) {
        // Every client retry triggers a new credit card deduction
        PaymentService.PaymentResult result =
                paymentService.executeCharge(request.customerId(), request.amount());
        return ResponseEntity.ok(result);
    }
}
