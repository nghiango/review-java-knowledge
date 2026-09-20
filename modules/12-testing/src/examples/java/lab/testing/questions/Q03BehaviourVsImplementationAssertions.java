package lab.testing.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import lab.testing.pricing.CheckoutService;
import lab.testing.pricing.LineItem;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import lab.testing.pricing.PricingCalculator;

/**
 * Q03: Behaviour assertions vs implementation assertions.
 *
 * <p>A behaviour assertion describes what the caller can observe — the amount {@code checkout}
 * returns and the amount the customer is charged. An implementation assertion describes how the
 * result was produced — how many internal steps ran, which collaborator was called. Both pass
 * today; only the behaviour assertion still passes after a behaviour-preserving refactor, which is
 * why the suite must be written in terms of the first.
 */
public class Q03BehaviourVsImplementationAssertions {

    /**
     * The pricing rule as an interface, so two behaviourally identical implementations can exist.
     */
    interface Pricing {

        Money total(List<LineItem> items);

        /** Implementation detail, deliberately exposed so the example can assert on it. */
        int internalSteps();
    }

    /** Prices line by line, keeping a running total. */
    static final class LineByLinePricing implements Pricing {

        private int steps;

        @Override
        public Money total(List<LineItem> items) {
            Money total = Money.ZERO;
            for (LineItem item : items) {
                steps++;
                total = total.plus(new Money(item.unitPriceCents()).multiply(item.quantity()));
            }
            return total;
        }

        @Override
        public int internalSteps() {
            return steps;
        }
    }

    /** Prices in one reduction: same result, different internals. */
    static final class StreamingPricing implements Pricing {

        private int steps;

        @Override
        public Money total(List<LineItem> items) {
            steps++;
            long cents = items.stream().mapToLong(i -> i.unitPriceCents() * i.quantity()).sum();
            return new Money(cents);
        }

        @Override
        public int internalSteps() {
            return steps;
        }
    }

    /** Hand-written spy: records what was charged so the observable effect can be asserted on. */
    static final class RecordingGateway implements PaymentGateway {

        private final List<Money> charges = new ArrayList<>();

        @Override
        public void charge(Money amount) {
            charges.add(amount);
        }

        List<Money> charges() {
            return List.copyOf(charges);
        }
    }

    public static void main(String[] args) {
        List<LineItem> basket =
                List.of(new LineItem("BOOK-001", 1999, 2), new LineItem("PEN-042", 250, 3));

        RecordingGateway gateway = new RecordingGateway();
        Money charged = new CheckoutService(new PricingCalculator(), gateway).checkout(basket, 20);

        // Behaviour: the amount returned and the amount the customer is charged.
        assertThat(charged).isEqualTo(new Money(3798)); // passes: 4748 cents minus 20%
        assertThat(gateway.charges()).containsExactly(new Money(3798)); // passes

        Pricing lineByLine = new LineByLinePricing();
        Pricing streaming = new StreamingPricing();
        boolean behaviourHolds = lineByLine.total(basket).equals(streaming.total(basket)); // true

        // The implementation assertion is not portable: it asserts one step per line, which is a
        // refactoring detail of one strategy rather than the contract both of them satisfy.
        boolean implHolds = lineByLine.internalSteps() == streaming.internalSteps(); // false
        int perLineSteps = lineByLine.internalSteps(); // 2
        int streamingSteps = streaming.internalSteps(); // 1

        // A behaviour assertion also covers the rejection path, which never reaches the gateway.
        CheckoutService service = new CheckoutService(new PricingCalculator(), gateway);
        assertThatThrownBy(() -> service.checkout(basket, 101))
                .isInstanceOf(IllegalArgumentException.class); // passes

        System.out.println("Charged: " + charged); // Charged: Money[cents=3798]
        System.out.println("Behaviour holds: " + behaviourHolds); // Behaviour holds: true
        System.out.println("Impl holds: " + implHolds); // Impl holds: false
        System.out.println("Per-line steps: " + perLineSteps); // Per-line steps: 2
        System.out.println("Streaming steps: " + streamingSteps); // Streaming steps: 1
    }
}
