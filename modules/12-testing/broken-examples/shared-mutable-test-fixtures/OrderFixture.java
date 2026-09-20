package lab.testing.broken.sharedmutabletestfixtures;

import java.util.ArrayList;
import java.util.List;
import lab.testing.orders.Order;
import lab.testing.orders.OrderLine;
import lab.testing.pricing.Money;

/**
 * The orders the order tests work with.
 *
 * <p>Kept in one place so that a test does not have to describe a basket of its own: ORD-1001 is two
 * BOOK-001 at 1999 cents, ORD-1002 is three PEN-042 at 250 cents, and SHIPPING is the flat delivery
 * charge the order tests use.
 */
final class OrderFixture {

    static final List<Order> ORDERS =
            new ArrayList<>(
                    List.of(
                            new Order("ORD-1001", List.of(new OrderLine("BOOK-001", 1999, 2))),
                            new Order("ORD-1002", List.of(new OrderLine("PEN-042", 250, 3)))));

    static final Money SHIPPING = new Money(499);

    private OrderFixture() {}

    static Order emptyOrder() {
        return new Order("ORD-1003", List.of());
    }

    static Order multiLineOrder() {
        return new Order(
                "ORD-1004",
                List.of(new OrderLine("BOOK-001", 1999, 2), new OrderLine("PEN-042", 250, 3)));
    }
}
