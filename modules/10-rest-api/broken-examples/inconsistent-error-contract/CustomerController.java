package lab.restapi.broken.problemdetails;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @GetMapping("/{id}")
    public ResponseEntity<Object> getCustomer(@PathVariable String id) {
        if (!id.matches("\\d+")) {
            // Error format 1: Plain text string error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: Customer ID must be numeric");
        }
        if ("999".equals(id)) {
            // Error format 2: Ad-hoc JSON map
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("errorMessage", "Customer 999 does not exist", "timestamp", System.currentTimeMillis()));
        }
        return ResponseEntity.ok(Map.of("id", Long.parseLong(id), "name", "Alice"));
    }

    @PostMapping
    public ResponseEntity<Object> createCustomer(@RequestBody Map<String, String> payload) {
        if (!payload.containsKey("email")) {
            // Error format 3: Custom nested map shape
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("errors", Map.of("field", "email", "reason", "Email required")));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }
}
