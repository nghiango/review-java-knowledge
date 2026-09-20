package lab.testing.broken.assertingimplementationnotbehaviour;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import lab.testing.pricing.LineItem;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import lab.testing.pricing.PricingCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private PricingCalculator calculator;

    @Mock private PaymentGateway gateway;

    @Test
    void checkoutUsesTheCalculatorThenChargesTheGateway() {
        List<LineItem> items =
                List.of(new LineItem("BOOK-001", 1999, 2), new LineItem("PEN-042", 250, 3));
        Money subtotal = new Money(4748);
        Money discounted = new Money(3798);
        when(calculator.subtotal(items)).thenReturn(subtotal);
        when(calculator.applyPercentDiscount(subtotal, 20)).thenReturn(discounted);

        CheckoutService service = new CheckoutService(calculator, gateway);
        service.checkout(items, 20);

        verify(calculator).subtotal(items);
        verify(calculator).applyPercentDiscount(subtotal, 20);
        verify(calculator, times(1)).subtotal(anyList());
        verify(gateway).charge(any(Money.class));

        InOrder inOrder = inOrder(calculator, gateway);
        inOrder.verify(calculator).subtotal(items);
        inOrder.verify(calculator).applyPercentDiscount(subtotal, 20);
        inOrder.verify(gateway).charge(any(Money.class));
    }
}
