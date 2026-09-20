# Solution: Wrong HTTP Status Codes (Envelope 200 Anti-Pattern)

## Annotated Code

### `ProductController.java`

```java
package lab.springmvc.broken.httpstatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping("/{id}")
    public ApiResponse<Product> getProduct(@PathVariable("id") String id) {
        if ("missing".equals(id)) {
            // API design issue: Returning HTTP 200 OK for missing resource breaks HTTP caching and client error handling
            return ApiResponse.error(404, "Product not found");
        }
        return ApiResponse.success(new Product(id, "Laptop", 1200.0));
    }

    @PostMapping
    public ApiResponse<Product> createProduct(@RequestBody Product product) {
        if (product.price() <= 0) {
            // API design issue: Returning HTTP 200 OK for bad client input disables standard 4xx error monitoring
            return ApiResponse.error(400, "Price must be positive");
        }
        // API design issue: Successful POST must return 201 Created with Location header, not generic 200 OK
        return ApiResponse.success(product);
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `ProductController.java:18` | `API design` | HTTP 200 for 404 Not Found | Intermediate proxies, load balancers, and CDNs cache 200 responses; client retry logic cannot trigger on standard 4xx/5xx status codes. |
| `ProductController.java:27` | `API design` | HTTP 200 for 400 Bad Request | Bypasses standard HTTP protocol error handling and forces clients to parse response body to detect failure. |
| `ProductController.java:30` | `API design` | Missing 201 Created and Location header | RESTful resource creation should return HTTP `201 Created` with a `Location: /api/products/{id}` header. |

## Correct implementation

- Package: `lab.springmvc.httpstatus`
- Production reference: `Product.java`, `ProductController.java`
- Fix: Leverage native HTTP status codes via `ResponseEntity.created(location).body(...)`, `ResponseEntity.ok(...)`, `ResponseEntity.noContent()`, and standard RFC 9457 `ProblemDetail` for errors (404, 400, 500).
