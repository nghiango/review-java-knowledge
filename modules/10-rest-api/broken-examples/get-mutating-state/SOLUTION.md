# Solution: GET Request Mutating State

## Annotated Code

### `OrderCancellationController.java`
```java
package lab.restapi.broken.safemethods;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderCancellationController {

    private final Map<Long, String> orderStore = new ConcurrentHashMap<>();

    public OrderCancellationController() {
        orderStore.put(101L, "CONFIRMED");
        orderStore.put(102L, "CONFIRMED");
    }

    // Reliability issue: GET method must be safe and read-only according to RFC 9110; state mutation over GET causes accidental cancellations from web crawlers, CDN prefetchers, and antivirus link inspection
    @GetMapping("/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrder(@PathVariable Long id) {
        String existingStatus = orderStore.get(id);
        if (existingStatus == null) {
            return ResponseEntity.notFound().build();
        }

        orderStore.put(id, "CANCELLED");
        return ResponseEntity.ok(Map.of("orderId", String.valueOf(id), "status", "CANCELLED"));
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| State Mutation on Safe HTTP Method | Critical | Reliability | RFC 9110 mandates that HTTP `GET` must be safe (no side effects). Using `GET` for write operations causes automated crawlers, browser link prefetching, email antivirus URL sandboxes, and CDN caching to trigger destructive mutations. |

---

## Remediation Strategy

1. **Option 1: Sub-resource State Transition with POST (Controller Action):**
   ```java
   @PostMapping("/{id}/cancellation")
   public ResponseEntity<OrderDto> cancelOrder(@PathVariable Long id) {
       OrderDto cancelledOrder = orderService.cancelOrder(id);
       return ResponseEntity.ok(cancelledOrder);
   }
   ```
2. **Option 2: DELETE Method (if cancelling represents resource decommissioning):**
   ```java
   @DeleteMapping("/{id}")
   public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
       orderService.cancelOrder(id);
       return ResponseEntity.noContent().build();
   }
   ```
3. **Option 3: PATCH Partial Update:**
   ```java
   @PatchMapping("/{id}")
   public ResponseEntity<OrderDto> updateStatus(
           @PathVariable Long id, @RequestBody UpdateOrderStatusRequest request) { ... }
   ```
