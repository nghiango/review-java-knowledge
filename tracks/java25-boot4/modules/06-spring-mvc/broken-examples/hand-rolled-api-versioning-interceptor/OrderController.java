package lab.java25boot4.springmvc.broken.apiversioning;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable String id, HttpServletRequest request) {
        String version = (String) request.getAttribute("resolvedVersion");
        if ("1.0".equals(version)) {
            return ResponseEntity.ok(new OrderV1Response(id, "LEGACY_STATUS"));
        } else if ("2.0".equals(version)) {
            return ResponseEntity.ok(new OrderV2Response(id, "FULFILLED", 9900));
        }
        return ResponseEntity.badRequest().body("Unsupported API version");
    }

    public record OrderV1Response(String orderId, String status) {}

    public record OrderV2Response(String orderId, String status, int amountCents) {}
}
