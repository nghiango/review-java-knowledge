package lab.springsecurity.idor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class InvoiceService {

    private final Map<UUID, Invoice> invoiceStore = new ConcurrentHashMap<>();

    public InvoiceService() {
        UUID inv1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID inv2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        invoiceStore.put(
                inv1, new Invoice(inv1, "alice", new BigDecimal("1500.00"), "Q1 Consulting"));
        invoiceStore.put(
                inv2, new Invoice(inv2, "bob", new BigDecimal("4200.00"), "Enterprise License"));
    }

    public Optional<Invoice> getInvoice(UUID id, String requesterUsername, boolean isAdmin) {
        Invoice invoice = invoiceStore.get(id);
        if (invoice == null) {
            return Optional.empty();
        }

        // BOLA / IDOR protection: Verify ownership or administrative authorization
        if (!isAdmin && !invoice.ownerUsername().equalsIgnoreCase(requesterUsername)) {
            throw new AccessDeniedException(
                    "User " + requesterUsername + " is not authorized to view invoice " + id);
        }

        return Optional.of(invoice);
    }
}
