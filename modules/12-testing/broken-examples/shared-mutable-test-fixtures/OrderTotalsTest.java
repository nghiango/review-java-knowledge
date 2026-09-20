package lab.testing.broken.sharedmutabletestfixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import lab.testing.orders.OrderTotals;
import lab.testing.pricing.Money;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests for {@link OrderTotals}.
 *
 * <p>The orders the tests work with come from {@link OrderFixture}, and the methods run in the order
 * declared here so the suite is deterministic in CI.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderTotalsTest {

    private final OrderTotals totals = new OrderTotals();

    @Test
    @Order(1)
    void total_singleLineOrder_returnsLineTotal() {
        assertEquals(new Money(3998), totals.total(OrderFixture.ORDERS.get(0)));
    }

    @Test
    @Order(2)
    void total_emptyOrder_returnsZero() {
        assertEquals(Money.ZERO, totals.total(OrderFixture.emptyOrder()));
    }

    @Test
    @Order(3)
    void totalWithShipping_addsShippingToOrderTotal() {
        assertEquals(
                new Money(1249),
                totals.totalWithShipping(OrderFixture.ORDERS.get(1), OrderFixture.SHIPPING));
    }

    @Test
    @Order(4)
    void total_multiLineOrder_sumsEveryLine() {
        OrderFixture.ORDERS.add(OrderFixture.multiLineOrder());

        assertEquals(
                new Money(4748), totals.total(OrderFixture.ORDERS.get(OrderFixture.ORDERS.size() - 1)));
    }

    @Test
    @Order(5)
    void totalWithShipping_largestOrder_usesFixtureShipping() {
        assertEquals(3, OrderFixture.ORDERS.size());

        assertEquals(
                new Money(5247),
                totals.totalWithShipping(OrderFixture.ORDERS.get(2), OrderFixture.SHIPPING));
    }
}
