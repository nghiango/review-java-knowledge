package lab.testing.pricing;

import java.util.List;
import java.util.Objects;

/**
 * Checkout use case: validates the basket, prices it and charges the customer.
 *
 * <p>Design decisions worth noting:
 *
 * <ul>
 *   <li>The pure {@link PricingCalculator} is injected and used for real; only the remote {@link
 *       PaymentGateway} is a boundary that a test may replace.
 *   <li>{@link #checkout} returns the amount actually charged, so the service has an observable
 *       result a caller (or test) can assert instead of inspecting internal calls.
 *   <li>A zero total (empty basket or a full discount) is not sent to the gateway: there is nothing
 *       to charge, and issuing a zero-value payment would only create noise in the provider.
 * </ul>
 */
public final class CheckoutService {

    private final PricingCalculator calculator;
    private final PaymentGateway gateway;

    public CheckoutService(PricingCalculator calculator, PaymentGateway gateway) {
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.gateway = Objects.requireNonNull(gateway, "gateway must not be null");
    }

    /**
     * Prices {@code items}, applies {@code discountPercent} and charges the customer.
     *
     * @return the amount charged; {@link Money#ZERO} when there is nothing to charge
     * @throws IllegalArgumentException if the basket is {@code null}, a quantity is negative, or
     *     the discount is outside 0..100
     */
    public Money checkout(List<LineItem> items, int discountPercent) {
        Objects.requireNonNull(items, "items must not be null");
        for (LineItem item : items) {
            if (item.quantity() < 0) {
                throw new IllegalArgumentException(
                        "quantity must not be negative for sku " + item.sku());
            }
        }

        Money subtotal = calculator.subtotal(items);
        Money total = calculator.applyPercentDiscount(subtotal, discountPercent);
        if (total.cents() > 0) {
            gateway.charge(total);
        }
        return total;
    }
}
