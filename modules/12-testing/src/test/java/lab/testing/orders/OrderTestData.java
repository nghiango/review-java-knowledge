package lab.testing.orders;

import java.util.ArrayList;
import java.util.List;

/**
 * Test-only builders for order data.
 *
 * <p>Every call to {@link #anOrder(String)} returns a fresh builder, so the value a test works with
 * exists only for that test: no test can observe or change another test's data, and no execution
 * order is required. Nothing here is static-mutable, which is what keeps the suite independent —
 * the builder is the whole fixture.
 */
final class OrderTestData {

    private OrderTestData() {}

    /** Starts a builder for the order with the given id and no lines. */
    static OrderBuilder anOrder(String id) {
        return new OrderBuilder(id);
    }

    /** Fluent builder for one {@link Order}; owns its line list until {@link #build()}. */
    static final class OrderBuilder {

        private final String id;
        private final List<OrderLine> lines = new ArrayList<>();

        private OrderBuilder(String id) {
            this.id = id;
        }

        /** Adds a line and returns this builder so lines can be chained. */
        OrderBuilder withLine(String sku, long unitPriceCents, int quantity) {
            lines.add(new OrderLine(sku, unitPriceCents, quantity));
            return this;
        }

        /**
         * Builds the order. {@link Order} copies the lines, so later builder use cannot affect it.
         */
        Order build() {
            return new Order(id, lines);
        }
    }
}
