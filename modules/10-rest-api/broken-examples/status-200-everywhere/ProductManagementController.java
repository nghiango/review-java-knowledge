package lab.restapi.broken.statuscodes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductManagementController {

    private final Map<Long, Map<String, Object>> productStore = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody Map<String, Object> request) {
        Long id = idSeq.getAndIncrement();
        Map<String, Object> product = new HashMap<>(request);
        product.put("id", id);
        productStore.put(id, product);

        // Always returns 200 OK; missing 201 Created and Location header
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long id) {
        Map<String, Object> product = productStore.get(id);
        if (product == null) {
            // Returns 200 OK for not found resource with embedded error payload
            return ResponseEntity.ok(Map.of("status", "ERROR", "code", 404, "message", "Not Found"));
        }
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        productStore.remove(id);
        // Returns 200 OK with custom payload instead of semantic 204 No Content
        return ResponseEntity.ok(Map.of("success", true));
    }
}
