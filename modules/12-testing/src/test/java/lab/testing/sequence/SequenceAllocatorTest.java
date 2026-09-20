package lab.testing.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link SequenceAllocator}.
 *
 * <p>Every test builds the allocator it asserts on, so the class holds no state of its own: the
 * methods pass alone, in any order, when the class runs twice in the same JVM, and when the suite
 * is executed in parallel. The allocator is deliberately not kept in a field or a static fixture —
 * sharing one counter between methods is exactly what turns a suite into a sequence.
 */
class SequenceAllocatorTest {

    @Test
    @DisplayName("the first allocation returns one more than the start value")
    void next_fromStart_returnsStartPlusOne() {
        assertThat(new SequenceAllocator(0).next()).isEqualTo(1);
    }

    @Test
    @DisplayName("repeated allocations return consecutive values")
    void next_calledRepeatedly_returnsConsecutiveValues() {
        SequenceAllocator allocator = new SequenceAllocator(10);

        assertThat(allocator.next()).isEqualTo(11);
        assertThat(allocator.next()).isEqualTo(12);
        assertThat(allocator.next()).isEqualTo(13);
    }

    @Test
    @DisplayName("a negative start value is honoured")
    void next_negativeStart_returnsConsecutiveValues() {
        SequenceAllocator allocator = new SequenceAllocator(-3);

        assertThat(allocator.next()).isEqualTo(-2);
        assertThat(allocator.next()).isEqualTo(-1);
    }

    @Test
    @DisplayName("current returns the start value before any allocation")
    void current_beforeAnyAllocation_returnsStart() {
        assertThat(new SequenceAllocator(5).current()).isEqualTo(5);
    }

    @Test
    @DisplayName("current returns the value handed out last")
    void current_afterAllocations_returnsLastAllocatedValue() {
        SequenceAllocator allocator = new SequenceAllocator(100);

        allocator.next();
        allocator.next();

        assertThat(allocator.current()).isEqualTo(102);
    }

    @Test
    @DisplayName("reset restarts the sequence at the start value")
    void reset_afterAllocations_restartsAtStart() {
        SequenceAllocator allocator = new SequenceAllocator(7);

        allocator.next();
        allocator.next();
        allocator.reset();

        assertThat(allocator.current()).isEqualTo(7);
        assertThat(allocator.next()).isEqualTo(8);
    }

    @Test
    @DisplayName("reset on an untouched allocator leaves it at the start value")
    void reset_beforeAnyAllocation_leavesAllocatorAtStart() {
        SequenceAllocator allocator = new SequenceAllocator(4);

        allocator.reset();

        assertThat(allocator.current()).isEqualTo(4);
        assertThat(allocator.next()).isEqualTo(5);
    }

    @Test
    @DisplayName("two allocators with the same start value advance independently")
    void next_twoAllocatorsWithSameStart_doNotShareState() {
        SequenceAllocator first = new SequenceAllocator(0);
        SequenceAllocator second = new SequenceAllocator(0);

        assertThat(first.next()).isEqualTo(1);

        assertThat(second.current()).isEqualTo(0);
        assertThat(second.next()).isEqualTo(1);
    }

    @Test
    @DisplayName("the last representable value is allocated before the counter runs out")
    void next_atMaxValueMinusOne_allocatesMaxValueThenRejectsTheNext() {
        SequenceAllocator allocator = new SequenceAllocator(Long.MAX_VALUE - 1);

        assertThat(allocator.next()).isEqualTo(Long.MAX_VALUE);

        assertThatThrownBy(allocator::next)
                .isInstanceOf(ArithmeticException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    @DisplayName("an exhausted allocator fails instead of wrapping to a negative value")
    void next_fromMaxValue_throwsInsteadOfWrapping() {
        assertThatThrownBy(new SequenceAllocator(Long.MAX_VALUE)::next)
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("reset recovers an allocator that has reached the boundary")
    void reset_afterExhaustion_restartsBelowTheBoundary() {
        SequenceAllocator allocator = new SequenceAllocator(Long.MAX_VALUE - 1);

        allocator.next();
        allocator.reset();

        assertThat(allocator.current()).isEqualTo(Long.MAX_VALUE - 1);
        assertThat(allocator.next()).isEqualTo(Long.MAX_VALUE);
    }
}
