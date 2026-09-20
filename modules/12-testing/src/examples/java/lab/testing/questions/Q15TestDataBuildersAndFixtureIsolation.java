package lab.testing.questions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import lab.testing.orders.Order;
import lab.testing.orders.OrderLine;
import lab.testing.orders.OrderTotals;
import lab.testing.pricing.Money;

/**
 * Q15: Test data builders and fixture isolation.
 *
 * <p>A builder is the whole fixture: {@code anOrder("ORD-1001").withLine(...).build()} hands each
 * test its own data, so no test can observe or change another test's input and no execution order
 * is required. The failure mode is a builder (or a fixture object) that is shared between tests:
 * the second test then builds on the first test's lines, which is exactly the leak the numbers
 * below show. {@code Order} copies its line list, so a shared *value* is safe where a shared
 * *builder* is not.
 */
public class Q15TestDataBuildersAndFixtureIsolation {

    /** A builder handed out fresh per test; it owns its line list until {@link #build()}. */
    static final class OrderBuilder {

        private final String id;
        private final List<OrderLine> lines = new ArrayList<>();

        private OrderBuilder(String id) {
            this.id = id;
        }

        static OrderBuilder anOrder(String id) {
            return new OrderBuilder(id);
        }

        OrderBuilder withLine(String sku, long unitPriceCents, int quantity) {
            lines.add(new OrderLine(sku, unitPriceCents, quantity));
            return this;
        }

        Order build() {
            return new Order(id, lines);
        }
    }

    public static void main(String[] args) {
        Order first = OrderBuilder.anOrder("ORD-1001").withLine("BOOK-001", 1999, 2).build();
        Order second = OrderBuilder.anOrder("ORD-1001").withLine("BOOK-001", 1999, 2).build();

        assertThat(new OrderTotals().total(first)).isEqualTo(new Money(3998)); // passes
        assertThat(second.lines()).isNotSameAs(first.lines()); // passes: build() copies the lines

        boolean equalValues = first.equals(second); // true: the same data builds the same order

        // A builder reused across two tests leaks the first test's lines into the second.
        OrderBuilder shared = OrderBuilder.anOrder("ORD-1002");
        Order fromFirstTest = shared.withLine("PEN-042", 250, 1).build();
        Order fromSecondTest = shared.withLine("MUG-007", 1250, 1).build();
        int firstTestLines = fromFirstTest.lines().size(); // 1
        int secondTestLines = fromSecondTest.lines().size(); // 2: the leak
        boolean reusedBuilderLeaks = secondTestLines != firstTestLines; // true

        // The order value itself is immutable, so a shared value cannot leak.
        String mutationOutcome;
        try {
            first.lines().add(new OrderLine("X-1", 1, 1));
            mutationOutcome = "mutated";
        } catch (UnsupportedOperationException e) {
            mutationOutcome = e.getClass().getSimpleName(); // "UnsupportedOperationException"
        }
        boolean linesAreImmutable = "UnsupportedOperationException".equals(mutationOutcome); // true

        System.out.println("Total: " + new OrderTotals().total(first)); // Total: Money[cents=3998]
        System.out.println("Equal values: " + equalValues); // Equal values: true
        System.out.println("First test lines: " + firstTestLines); // First test lines: 1
        System.out.println("Second test lines: " + secondTestLines); // Second test lines: 2
        System.out.println(
                "Reused builder leaks: " + reusedBuilderLeaks); // Reused builder leaks: true
        System.out.println("Lines immutable: " + linesAreImmutable); // Lines immutable: true
    }
}
