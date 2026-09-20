package lab.springsecurity.broken.idor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final Map<UUID, Invoice> invoiceStore = new ConcurrentHashMap<>();

    public InvoiceController() {
        UUID inv1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID inv2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        invoiceStore.put(inv1, new Invoice(inv1, "alice", new BigDecimal("1500.00"), "Q1 Consulting"));
        invoiceStore.put(inv2, new Invoice(inv2, "bob", new BigDecimal("4200.00"), "Enterprise License"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable UUID id) {
        // Blindly fetches invoice by ID from URL without verifying if the requesting user owns it
        Invoice invoice = invoiceStore.get(id);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(invoice);
    }
}
