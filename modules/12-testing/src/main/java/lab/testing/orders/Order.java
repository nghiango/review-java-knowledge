package lab.testing.orders;

import java.util.List;
import java.util.Objects;

/**
 * A customer order: an id and the lines it contains.
 *
 * <p>The compact constructor takes a defensive copy of the lines, so an order is immutable and two
 * tests can safely share the same order value — no test can change data another test asserts on.
 * That is the value-level half of the isolation the test suite relies on; the other half is that
 * each test builds its own order (see {@code OrderTestData}).
 */
public record Order(String id, List<OrderLine> lines) {

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        lines = List.copyOf(Objects.requireNonNull(lines, "lines must not be null"));
    }
}
