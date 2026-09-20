package lab.testing.pricing;

/**
 * One basket line: a SKU, the unit price in cents and the ordered quantity.
 *
 * <p>This is a plain value carrier. Basket validation deliberately lives at the {@link
 * CheckoutService} boundary rather than in the record, so a malformed request (for example a
 * negative quantity) is reported as a rejected checkout instead of an object-construction failure.
 */
public record LineItem(String sku, long unitPriceCents, int quantity) {}
