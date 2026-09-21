package lab.java25boot4.restapi.questions;

import java.util.List;

/**
 * Q07: How does Jackson 3 modernize JSON serialization for records and immutable collections in
 * Spring Boot 4 REST APIs?
 */
public class Q07Jackson3RestPayloadEvolutionExample {

    public record ImmutableInvoicePayload(
            String invoiceId,
            String customerEmail,
            long totalAmountCents,
            List<String> lineItemIds) {

        public ImmutableInvoicePayload {
            lineItemIds = List.copyOf(lineItemIds); // Defensive copy enforcing immutability
        }
    }

    public static void main(String[] args) {
        var items = List.of("ITEM-1", "ITEM-2");
        var invoice = new ImmutableInvoicePayload("INV-100", "billing@example.com", 25000L, items);

        boolean isImmutable =
                invoice.lineItemIds().getClass().getName().contains("Immutable")
                        || invoice.lineItemIds().getClass().getName().contains("ListN");

        System.out.println("Invoice ID: " + invoice.invoiceId()); // "INV-100"
        System.out.println("Item count: " + invoice.lineItemIds().size()); // 2
        System.out.println("Collection immutable: " + isImmutable); // true
    }
}
