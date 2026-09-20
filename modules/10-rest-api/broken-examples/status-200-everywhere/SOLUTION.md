# Solution: Status 200 Everywhere

## Annotated Code

### `ProductManagementController.java`
```java
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

        // Maintainability issue: Resource creation should return HTTP 201 Created with a Location header pointing to the newly created URI
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long id) {
        Map<String, Object> product = productStore.get(id);
        if (product == null) {
            // Correctness issue: Returning 200 OK for missing resources violates HTTP status code semantics, breaks client error interceptors, and prevents caching proxies from caching 404s
            return ResponseEntity.ok(Map.of("status", "ERROR", "code", 404, "message", "Not Found"));
        }
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProduct(@PathVariable Long id) {
        productStore.remove(id);
        // Maintainability issue: Successful resource deletion without response body should return HTTP 204 No Content
        return ResponseEntity.ok(Map.of("success", true));
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| HTTP 200 Returned for Resource Errors | High | Correctness | Masking `404 Not Found` or `400 Bad Request` inside `200 OK` responses breaks standard HTTP clients (e.g. `RestClient`, `WebClient`, Axios), API gateways, and APM alerting tools. |
| Missing 201 Created and Location Header | Medium | Maintainability | `POST` creation requests must return `201 Created` with the canonical URI in the `Location` header (`/api/products/{id}`). |
| Missing 204 No Content on Deletions | Low | Maintainability | Deletions that return no payload should respond with `204 No Content`. |

---

## Remediation Strategy

1. **Use Semantic HTTP Status Codes:**
   - Resource Created: `201 Created` with `Location` header:
     ```java
     URI location = ServletUriComponentsBuilder.fromCurrentRequest()
             .path("/{id}").buildAndExpand(product.id()).toUri();
     return ResponseEntity.created(location).body(product);
     ```
   - Resource Not Found: Return standard `404 Not Found` (or RFC 9457 `ProblemDetail`).
   - Resource Deleted: `204 No Content`:
     ```java
     return ResponseEntity.noContent().build();
     ```
