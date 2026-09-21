package lab.java25boot4.corejava;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StreamGatherersTest {

    @Test
    @DisplayName("windowFixed partitions stream into uniform chunks")
    void shouldPartitionStreamUsingWindowFixed() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<List<Integer>> batches = StreamGatherersDemo.fixedBatches(numbers, 3);

        assertThat(batches).containsExactly(List.of(1, 2, 3), List.of(4, 5, 6), List.of(7, 8));
    }

    @Test
    @DisplayName("windowSliding creates overlapping windows")
    void shouldSlideWindowAcrossElements() {
        List<Integer> numbers = List.of(10, 20, 30, 40);
        List<List<Integer>> windows = StreamGatherersDemo.slidingWindows(numbers, 2);

        assertThat(windows).containsExactly(List.of(10, 20), List.of(20, 30), List.of(30, 40));
    }

    @Test
    @DisplayName("scan calculates running cumulative accumulation")
    void shouldCalculateRunningSumWithScan() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        List<Integer> cumulative = StreamGatherersDemo.runningSum(numbers);

        assertThat(cumulative).containsExactly(1, 3, 6, 10, 15);
    }
}
