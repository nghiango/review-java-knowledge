package lab.testing.pricing;

/**
 * An immutable monetary amount in minor units (cents).
 *
 * <p>Money is a value object: every operation returns a new instance and negative amounts are
 * rejected in the compact constructor, so an invalid price cannot exist anywhere in the checkout
 * flow. Integer arithmetic is used throughout so that rounding is exact and reproducible in tests.
 */
public record Money(long cents) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + cents);
        }
    }

    /** Returns this amount increased by {@code other}. */
    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    /** Returns this amount multiplied by a non-negative {@code factor}. */
    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("factor cannot be negative: " + factor);
        }
        return new Money(Math.multiplyExact(cents, factor));
    }
}
