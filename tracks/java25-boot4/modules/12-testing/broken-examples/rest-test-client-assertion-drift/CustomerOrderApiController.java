package lab.java25boot4.testing.broken.assertiondrift;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-orders")
public class CustomerOrderApiController {

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id) {
        if ("invalid".equalsIgnoreCase(id)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "Malformed order identifier supplied");
            problem.setTitle("Invalid Order Identifier");
            problem.setType(URI.create("https://api.example.com/errors/invalid-id"));
            problem.setProperty("errorCode", "ERR_INVALID_ORD_ID");
            return ResponseEntity.badRequest().body(problem);
        }

        return ResponseEntity.ok(new OrderRecord(id, 4500L, "COMPLETED"));
    }

    public record OrderRecord(String id, long amountCents, String status) {}
}
