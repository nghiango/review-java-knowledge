package lab.java25boot4.testing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modern REST controller demonstrating RFC 9457 ProblemDetail exception handling, JSpecify
 * annotations, and immutable records.
 */
@RestController
@RequestMapping("/api/v2/orders")
public class ModernOrderApiController {

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotBlank String sku,
            @Min(1) int quantity,
            @Nullable String promoCode) {}

    public record OrderResponse(
            String orderId, String customerId, String sku, int quantity, String status) {}

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String orderId) {
            super("Order not found: " + orderId);
        }
    }

    public static class InvalidOrderPromoException extends RuntimeException {
        private final String promoCode;

        public InvalidOrderPromoException(String promoCode) {
            super("Promo code is invalid or expired: " + promoCode);
            this.promoCode = promoCode;
        }

        public String getPromoCode() {
            return promoCode;
        }
    }

    private final Map<String, OrderResponse> orderStore = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        if ("EXPIRED".equalsIgnoreCase(request.promoCode())) {
            throw new InvalidOrderPromoException(request.promoCode());
        }

        String orderId = "ord-" + System.nanoTime();
        OrderResponse order =
                new OrderResponse(
                        orderId,
                        request.customerId(),
                        request.sku(),
                        request.quantity(),
                        "CONFIRMED");
        orderStore.put(orderId, order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse order = orderStore.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return ResponseEntity.ok(order);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFound(OrderNotFoundException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setType(URI.create("https://api.example.com/errors/order-not-found"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InvalidOrderPromoException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPromo(InvalidOrderPromoException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Invalid Promo Code");
        problem.setType(URI.create("https://api.example.com/errors/invalid-promo"));
        problem.setProperty("promoCode", ex.getPromoCode());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }
}
