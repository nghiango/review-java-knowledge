package lab.java25boot4.restapi.broken.adhocversioning;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v-orders")
public class OrderResourceController {

    @GetMapping("/{id}")
    public ResponseEntity<?> fetchOrder(
            @PathVariable String id,
            @RequestHeader(value = "X-API-Version", required = false) String version) {
        if (version == null || version.equals("1")) {
            LegacyOrderDto dto = new LegacyOrderDto(id, "CUST-100", 150.0, "PROCESSING");
            return ResponseEntity.ok(dto);
        } else if (version.equals("2")) {
            ModernOrderRepresentation rep =
                    new ModernOrderRepresentation(id, "CUST-100", 15000L, "PROCESSING", "USD");
            return ResponseEntity.ok(rep);
        } else {
            throw new IllegalArgumentException("Unsupported API version requested: " + version);
        }
    }

    public record ModernOrderRepresentation(
            String orderId,
            String customerId,
            long amountCents,
            String status,
            String currency) {}
}
