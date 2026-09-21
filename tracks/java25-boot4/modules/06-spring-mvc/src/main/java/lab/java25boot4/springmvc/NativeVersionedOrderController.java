package lab.java25boot4.springmvc;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demonstrates native declarative API versioning in Spring MVC. Dispatches distinct requests
 * according to version parameters/headers.
 */
@RestController
@RequestMapping("/api/orders")
public class NativeVersionedOrderController {

    public record OrderV1(String id, String status) {}

    public record OrderV2(String id, String status, int amountCents, @Nullable String note) {}

    public record CreateOrderRequest(String itemId, int quantity) {}

    // Version 1.0 mapping
    @GetMapping(value = "/{id}", headers = "X-API-Version=1.0")
    public ResponseEntity<OrderV1> getOrderV1(@PathVariable String id) {
        return ResponseEntity.ok(new OrderV1(id, "PROCESSED_V1"));
    }

    // Version 2.0 mapping with richer model
    @GetMapping(value = "/{id}", headers = "X-API-Version=2.0")
    public ResponseEntity<OrderV2> getOrderV2(
            @PathVariable String id, @RequestParam(required = false) @Nullable String note) {
        return ResponseEntity.ok(new OrderV2(id, "FULFILLED_V2", 15000, note));
    }

    @PostMapping(headers = "X-API-Version=2.0")
    public ResponseEntity<OrderV2> createOrderV2(@RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(
                new OrderV2("ORD-" + request.itemId(), "CREATED", request.quantity() * 100, null));
    }
}
