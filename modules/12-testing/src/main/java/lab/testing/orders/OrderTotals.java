package lab.testing.orders;

import java.util.Objects;
import lab.testing.pricing.Money;

/**
 * Totals an order's lines, optionally with a shipping charge.
 *
 * <p>Stateless: every method is a pure function of its arguments, which is what allows the tests to
 * construct one instance per test and assert on returned values only.
 *
 * <p>Line arithmetic is written out here instead of being delegated to {@link
 * lab.testing.pricing.PricingCalculator}: that calculator works on the pricing example's {@code
 * LineItem}, and adapting between two structurally identical line records would couple two
 * independent examples for a one-line sum. {@link Money} — the type that owns the cent arithmetic
 * and its invariants — is deliberately shared.
 */
public final class OrderTotals {

    /** Returns the sum of unit price x quantity across every line of {@code order}. */
    public Money total(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        Money total = Money.ZERO;
        for (OrderLine line : order.lines()) {
            total = total.plus(new Money(line.unitPriceCents()).multiply(line.quantity()));
        }
        return total;
    }

    /** Returns {@link #total(Order)} plus {@code shipping}. */
    public Money totalWithShipping(Order order, Money shipping) {
        Objects.requireNonNull(shipping, "shipping must not be null");
        return total(order).plus(shipping);
    }
}
