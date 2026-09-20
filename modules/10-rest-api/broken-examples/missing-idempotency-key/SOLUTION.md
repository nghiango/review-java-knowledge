# Solution: Missing Idempotency Key in Payment Processing

## Annotated Code

### `PaymentCheckoutController.java`
```java
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

    // Reliability issue: Non-idempotent POST without Idempotency-Key validation causes duplicate credit card charges and financial discrepancies when clients retry on network timeouts
    @PostMapping("/charge")
    public ResponseEntity<PaymentService.PaymentResult> chargeCard(
            @RequestBody ChargeRequest request) {
        // Every client retry triggers a new credit card deduction
        PaymentService.PaymentResult result =
                paymentService.executeCharge(request.customerId(), request.amount());
        return ResponseEntity.ok(result);
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Duplicate Processing on Network Retries | Critical | Reliability | In distributed systems, network packets can be lost in transit after a server executes a command. Without an idempotency key filter, automatic client retries re-execute financial transactions, resulting in double-charging. |

---

## Remediation Strategy

1. **Require `Idempotency-Key` Header:**
   Clients supply a unique UUID or client-generated token in the `Idempotency-Key` header.
2. **Atomic Idempotency State Machine:**
   - **Step 1 (Check / Lock):** Check database or Redis cache for existing `idempotency_key`. If status is `IN_PROGRESS`, return `409 Conflict` (or wait).
   - **Step 2 (Execute):** If key is absent, insert key with `status = 'IN_PROGRESS'` and execute charge.
   - **Step 3 (Store Response):** Store final HTTP response body and status in database.
   - **Step 4 (Replay):** If key already exists with `status = 'COMPLETED'`, immediately return the cached response with header `Idempotency-Replayed: true`.
