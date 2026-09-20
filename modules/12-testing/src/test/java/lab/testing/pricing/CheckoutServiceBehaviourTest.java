package lab.testing.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Behaviour tests for {@link CheckoutService}.
 *
 * <p>Every assertion is made on what the caller can observe: the amount {@code checkout} returns
 * and the amount it charges. The real {@link PricingCalculator} is exercised, so the pricing and
 * rounding rules are actually tested; only the remote {@link PaymentGateway} boundary is doubled.
 * Nothing here asserts call order, call counts or internal delegation, so the tests survive any
 * refactor that preserves behaviour.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceBehaviourTest {

    @Mock private PaymentGateway gateway;

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    @DisplayName("a mixed basket is priced as unit price x quantity and the discount is charged")
    void checkout_mixedQuantities_chargesDiscountedSubtotal() {
        List<LineItem> items =
                List.of(new LineItem("BOOK-001", 1999, 2), new LineItem("PEN-042", 250, 3));

        Money charged = new CheckoutService(calculator, gateway).checkout(items, 20);

        // 1999 x 2 + 250 x 3 = 4748 cents, minus 20% = 3798 cents.
        assertThat(charged).isEqualTo(new Money(3798));
        verify(gateway).charge(new Money(3798));
    }

    @Test
    @DisplayName("a percentage discount rounds half up to the nearest cent")
    void checkout_discountWithFractionOfCent_roundsHalfUp() {
        List<LineItem> items = List.of(new LineItem("MUG-007", 1250, 1));

        Money charged = new CheckoutService(calculator, gateway).checkout(items, 33);

        // 1250 - 33% = 837.5 cents, rounded half up to 838.
        assertThat(charged).isEqualTo(new Money(838));
        verify(gateway).charge(new Money(838));
    }

    @Test
    @DisplayName("an empty basket costs nothing and the gateway is never called")
    void checkout_emptyBasket_chargesNothing() {
        Money charged = new CheckoutService(calculator, gateway).checkout(List.of(), 10);

        assertThat(charged).isEqualTo(Money.ZERO);
        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("a fully discounted basket costs nothing and the gateway is never called")
    void checkout_fullDiscount_chargesNothing() {
        List<LineItem> items = List.of(new LineItem("TRIAL-001", 500, 1));

        Money charged = new CheckoutService(calculator, gateway).checkout(items, 100);

        assertThat(charged).isEqualTo(Money.ZERO);
        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("a negative quantity is rejected before any payment is attempted")
    void checkout_negativeQuantity_rejected() {
        List<LineItem> items = List.of(new LineItem("BOOK-001", 1999, -1));
        CheckoutService service = new CheckoutService(calculator, gateway);

        assertThatThrownBy(() -> service.checkout(items, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("a discount outside 0..100 is rejected before any payment is attempted")
    void checkout_discountOutsideRange_rejected() {
        List<LineItem> items = List.of(new LineItem("BOOK-001", 1999, 1));
        CheckoutService service = new CheckoutService(calculator, gateway);

        assertThatThrownBy(() -> service.checkout(items, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("percent");

        verifyNoInteractions(gateway);
    }
}
