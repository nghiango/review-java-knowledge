package lab.testing.broken.assertingimplementationnotbehaviour;

import java.util.List;
import lab.testing.pricing.LineItem;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import lab.testing.pricing.PricingCalculator;

/** Prices a basket and charges the customer through the payment gateway. */
public final class CheckoutService {

    private final PricingCalculator calculator;
    private final PaymentGateway gateway;

    public CheckoutService(PricingCalculator calculator, PaymentGateway gateway) {
        this.calculator = calculator;
        this.gateway = gateway;
    }

    public Money checkout(List<LineItem> items, int discountPercent) {
        Money subtotal = calculator.subtotal(items);
        Money total = calculator.applyPercentDiscount(subtotal, discountPercent);
        gateway.charge(subtotal);
        return total;
    }
}
