package lab.testing.questions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import lab.testing.pricing.CheckoutService;
import lab.testing.pricing.LineItem;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import lab.testing.pricing.PricingCalculator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Q09: Mockito argument captors and verification.
 *
 * <p>{@code verify(mock).charge(new Money(3798))} proves the argument *equals* an expected value;
 * an {@link ArgumentCaptor} hands the argument to the test so it can be inspected when the expected
 * value is not known up front (a generated id, a timestamp, a computed amount). Verification itself
 * comes in four shapes: no-argument, matchers, cardinality, and {@code verifyNoMoreInteractions} —
 * and only the first two describe behaviour, so the others are used sparingly.
 */
public class Q09MockitoArgumentCaptorsAndVerification {

    public static void main(String[] args) {
        PaymentGateway gateway = mock(PaymentGateway.class);
        CheckoutService service = new CheckoutService(new PricingCalculator(), gateway);
        List<LineItem> basket =
                List.of(new LineItem("BOOK-001", 1999, 2), new LineItem("PEN-042", 250, 3));

        Money charged = service.checkout(basket, 20); // Money[cents=3798]

        // A captor records the argument the collaborator actually received.
        ArgumentCaptor<Money> captor = ArgumentCaptor.forClass(Money.class);
        verify(gateway).charge(captor.capture());
        Money captured = captor.getValue(); // Money[cents=3798]
        boolean capturedMatchesReturned = captured.equals(charged); // true

        // Verification with a matcher, an explicit cardinality, and the negative case.
        verify(gateway, times(1)).charge(any(Money.class));
        verify(gateway, never()).charge(Money.ZERO);
        verifyNoMoreInteractions(gateway); // the gateway was not called in any other way

        // A second checkout: a fresh captor collects every argument, in invocation order.
        service.checkout(basket, 0); // Money[cents=4748]
        ArgumentCaptor<Money> allCaptor = ArgumentCaptor.forClass(Money.class);
        verify(gateway, times(2)).charge(allCaptor.capture());
        List<Money> allCharges = allCaptor.getAllValues(); // 2 values, oldest first
        boolean chargesAreOrdered = allCharges.equals(List.of(charged, new Money(4748))); // true
        long invocations = Mockito.mockingDetails(gateway).getInvocations().size(); // 2

        System.out.println("Charged: " + charged); // Charged: Money[cents=3798]
        System.out.println("Captured: " + captured); // Captured: Money[cents=3798]
        System.out.println("Captor matches: " + capturedMatchesReturned); // Captor matches: true
        System.out.println("Charge count: " + allCharges.size()); // Charge count: 2
        System.out.println("Ordered: " + chargesAreOrdered); // Ordered: true
        System.out.println("Invocations: " + invocations); // Invocations: 2
    }
}
