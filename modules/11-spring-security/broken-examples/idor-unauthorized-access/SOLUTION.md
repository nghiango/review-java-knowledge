# Solution: Insecure Direct Object Reference (IDOR / BOLA)

## Annotated Code

### `InvoiceController.java`
```java
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

    // Security issue: Insecure Direct Object Reference (IDOR / BOLA, OWASP API1:2023). Endpoint directly trusts path variable 'id' without cross-checking the requester's authenticated principal.
    // Security issue: Any authenticated user (e.g. 'bob') can enumerate or access invoices belonging to 'alice' (or vice-versa).
    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceStore.get(id);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(invoice);
    }
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Insecure Direct Object Reference (IDOR / BOLA) | Security | Critical | The controller exposes private financial records directly through user-supplied identifier parameters (`UUID id`) without verifying that the requesting user (`Authentication.getName()` or `@AuthenticationPrincipal`) owns the resource or has administrative roles (CWE-639 / OWASP API1:2023). |
| Missing Authorization Layer Check | Security | Critical | Authentication alone does not guarantee authorization. Knowing a resource's ID allows horizontal privilege escalation across tenant accounts. |

## Correct Implementation Reference
- [`SafeInvoiceController.java`](../../src/main/java/lab/springsecurity/idor/SafeInvoiceController.java)
- [`InvoiceService.java`](../../src/main/java/lab/springsecurity/idor/InvoiceService.java)
