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

    // HTTP GET method used to perform a mutating state transition
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
