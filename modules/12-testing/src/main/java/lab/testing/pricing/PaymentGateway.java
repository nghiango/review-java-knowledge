package lab.testing.pricing;

/**
 * The outbound payment boundary.
 *
 * <p>This is the only collaborator of the checkout flow that performs remote I/O, so it is the only
 * one a test should replace with a test double.
 */
@FunctionalInterface
public interface PaymentGateway {

    /**
     * Charges the given amount to the customer.
     *
     * @param amount the amount to charge; never {@code null}
     */
    void charge(Money amount);
}
