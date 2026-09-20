package lab.testing.pricing;

import java.util.List;
import java.util.Objects;

/**
 * Pure pricing rules: line pricing, subtotal and percentage discount.
 *
 * <p>The calculator has no I/O and no side effects, which is exactly why it should be used for real
 * in tests: it owns the business rule under test, so replacing it with a mock would remove the
 * behaviour the test claims to verify.
 */
public final class PricingCalculator {

    /** Sums unit price x quantity across every line. */
    public Money subtotal(List<LineItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        Money total = Money.ZERO;
        for (LineItem item : items) {
            total = total.plus(new Money(item.unitPriceCents()).multiply(item.quantity()));
        }
        return total;
    }

    /**
     * Applies a percentage discount and rounds half up to the nearest cent.
     *
     * <p>Rounding uses integer arithmetic ({@code (value + 50) / 100}) so the result never depends
     * on floating-point representation. The {@code + 50} makes an exact half-cent round up.
     *
     * @param amount the amount to discount
     * @param percent the discount percentage, between 0 and 100 inclusive
     */
    public Money applyPercentDiscount(Money amount, int percent) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100: " + percent);
        }
        long discountedCents = (amount.cents() * (100L - percent) + 50L) / 100L;
        return new Money(discountedCents);
    }
}
