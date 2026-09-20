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

    // One allocator for the class, so a test does not have to build its own.
    private static final SequenceAllocator ALLOCATOR = new SequenceAllocator(100);

    @Test
    @Order(1)
    void next_fromStart_returnsFirstValue() {
        assertEquals(101, ALLOCATOR.next());
    }

    @Test
    @Order(2)
    void next_afterFirstAllocation_returnsSecondValue() {
        assertEquals(102, ALLOCATOR.next());
    }

    @Test
    @Order(3)
    void current_afterTwoAllocations_returnsLastValue() {
        assertEquals(102, ALLOCATOR.current());
    }

    @Test
    @Order(4)
    void next_afterTwoAllocations_returnsThirdValue() {
        assertEquals(103, ALLOCATOR.next());
    }

    @Test
    @Order(5)
    void reset_afterThreeAllocations_restartsAtStart() {
        ALLOCATOR.reset();

        assertEquals(100, ALLOCATOR.current());
    }
}
