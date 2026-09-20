package lab.testing.orders;

import java.util.Objects;

/**
 * One order line: a SKU, the unit price in cents and the ordered quantity.
 *
 * <p>The invariants live in the record rather than at the service boundary, so a malformed line
 * cannot exist and every total is computed from valid data. (The pricing example validates at its
 * request boundary instead, because there the malformed input is a caller-supplied basket.)
 */
public record OrderLine(String sku, long unitPriceCents, int quantity) {

    public OrderLine {
        Objects.requireNonNull(sku, "sku must not be null");
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException(
                    "unitPriceCents cannot be negative: " + unitPriceCents);
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative: " + quantity);
        }
    }
}
