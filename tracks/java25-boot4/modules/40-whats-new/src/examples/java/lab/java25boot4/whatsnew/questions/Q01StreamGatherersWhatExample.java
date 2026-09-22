package lab.java25boot4.whatsnew.questions;

import java.util.List;
import java.util.stream.Gatherers;

/** Q1: what does {@code Stream::gather} add that {@code map} and {@code filter} cannot? */
public class Q01StreamGatherersWhatExample {

    public static void main(String[] args) {
        List<Integer> input = List.of(1, 2, 3, 4, 5);

        // map is one-in / one-out; filter is one-in / zero-or-one-out
        List<Integer> mapped = input.stream().map(n -> n * 2).toList();

        // gather is stateful and one-in / many-out: windowFixed buffers and emits batches
        List<List<Integer>> batched = input.stream().gather(Gatherers.windowFixed(2)).toList();

        System.out.println(mapped); // [2, 4, 6, 8, 10]
        System.out.println(batched); // [[1, 2], [3, 4], [5]]
        System.out.println(batched.size()); // 3 — the trailing partial window is emitted by the finisher
    }
}
