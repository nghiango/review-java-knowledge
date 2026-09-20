package lab.restapi.idempotency;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class IdempotentPaymentController {

    private final IdempotentPaymentService paymentService;

    public IdempotentPaymentController(IdempotentPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charges")
    public ResponseEntity<PaymentResponse> charge(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        var result = paymentService.processPayment(idempotencyKey, request);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        headers.set("Idempotency-Replayed", String.valueOf(result.wasReplayed()));

        if (result.wasReplayed()) {
            return ResponseEntity.status(HttpStatus.OK).headers(headers).body(result.response());
        }

        URI location = URI.create("/api/payments/charges/" + result.response().transactionId());
        return ResponseEntity.created(location).headers(headers).body(result.response());
    }

    @ExceptionHandler(ConcurrentPaymentProcessingException.class)
    public ResponseEntity<String> handleConflict(ConcurrentPaymentProcessingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
