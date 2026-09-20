package lab.testing.broken.orderdependenttestsuite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests for {@link SequenceAllocator}.
 *
 * <p>The methods run in the order declared here so the suite is deterministic in CI.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SequenceAllocatorTest {

    @Test
    @Order(1)
    void next_fromStart_returnsFirstValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(1, allocator.next());
    }

    @Test
    @Order(2)
    void next_afterFirstAllocation_returnsSecondValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(2, allocator.next());
    }

    @Test
    @Order(3)
    void current_afterTwoAllocations_returnsLastValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(2, allocator.current());
    }

    @Test
    @Order(4)
    void next_afterTwoAllocations_returnsThirdValue() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        assertEquals(3, allocator.next());
    }

    @Test
    @Order(5)
    void reset_afterThreeAllocations_restartsAtStart() {
        SequenceAllocator allocator = new SequenceAllocator(0);

        allocator.reset();

        assertEquals(0, allocator.current());
    }
}
