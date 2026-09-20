package lab.restapi.broken.idempotency;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public record PaymentResult(String paymentId, String status, BigDecimal amount) {}

    public PaymentResult executeCharge(String customerId, BigDecimal amount) {
        // Simulates external credit card gateway charge execution
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        return new PaymentResult(paymentId, "SUCCEEDED", amount);
    }
}
