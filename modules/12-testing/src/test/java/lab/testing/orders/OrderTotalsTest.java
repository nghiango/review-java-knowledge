package lab.testing.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lab.testing.pricing.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link OrderTotals}.
 *
 * <p>Every test builds the order it needs through {@link OrderTestData}, so the input of a test is
 * visible in that test and nowhere else. There is no shared fixture, no static state and no
 * {@code @TestMethodOrder}: the class passes in any order, when a single method runs alone, and
 * when methods execute in parallel. {@link Money} is the only shared type, and it is immutable.
 */
class OrderTotalsTest {

    private final OrderTotals totals = new OrderTotals();

    @Test
    @DisplayName("a single-line order totals unit price x quantity")
    void total_singleLineOrder_returnsLineTotal() {
        Order order = OrderTestData.anOrder("ORD-1001").withLine("BOOK-001", 1999, 2).build();

        assertThat(totals.total(order)).isEqualTo(new Money(3998));
    }

    @Test
    @DisplayName("a multi-line order sums every line")
    void total_multipleLines_sumsEveryLine() {
        Order order =
                OrderTestData.anOrder("ORD-1002")
                        .withLine("BOOK-001", 1999, 2)
                        .withLine("PEN-042", 250, 3)
                        .build();

        assertThat(totals.total(order)).isEqualTo(new Money(4748));
    }

    @Test
    @DisplayName("an order with no lines totals zero")
    void total_emptyOrder_returnsZero() {
        Order order = OrderTestData.anOrder("ORD-1003").build();

        assertThat(totals.total(order)).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("a line with quantity zero contributes nothing")
    void total_zeroQuantityLine_contributesNothing() {
        Order order = OrderTestData.anOrder("ORD-1004").withLine("PEN-042", 250, 0).build();

        assertThat(totals.total(order)).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("shipping is added to the total of the order")
    void totalWithShipping_orderWithLines_addsShipping() {
        Order order = OrderTestData.anOrder("ORD-1005").withLine("PEN-042", 250, 3).build();

        assertThat(totals.totalWithShipping(order, new Money(499))).isEqualTo(new Money(1249));
    }

    @Test
    @DisplayName("shipping is charged even when the order has no lines")
    void totalWithShipping_emptyOrder_returnsShippingOnly() {
        Order order = OrderTestData.anOrder("ORD-1006").build();

        assertThat(totals.totalWithShipping(order, new Money(499))).isEqualTo(new Money(499));
    }

    @Test
    @DisplayName("a null order or shipping amount is rejected instead of totalling silently")
    void total_nullInput_rejected() {
        Order order = OrderTestData.anOrder("ORD-1007").build();

        assertThatThrownBy(() -> totals.total(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> totals.totalWithShipping(order, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("two independently built orders with the same data are equal and share no state")
    void testData_builtTwice_producesEqualIndependentOrders() {
        Order first = OrderTestData.anOrder("ORD-1008").withLine("BOOK-001", 1999, 1).build();
        Order second = OrderTestData.anOrder("ORD-1008").withLine("BOOK-001", 1999, 1).build();

        assertThat(second).isEqualTo(first);
        assertThat(first.lines()).isNotSameAs(second.lines());
    }

    @Test
    @DisplayName("an order's lines cannot be mutated through the record accessor")
    void order_linesAreImmutable() {
        Order order = OrderTestData.anOrder("ORD-1009").withLine("BOOK-001", 1999, 1).build();
        OrderLine extraLine = new OrderLine("PEN-042", 250, 1);

        assertThatThrownBy(() -> order.lines().add(extraLine))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
