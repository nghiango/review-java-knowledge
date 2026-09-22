package lab.java25boot4.whatsnew.questions;

import java.util.List;
import java.util.stream.Gatherers;

/** Q9: {@code windowFixed} vs {@code windowSliding} vs {@code scan} vs {@code fold}. */
public class Q09StreamGatherersWindowingExample {

    public static void main(String[] args) {
        List<Integer> samples = List.of(1, 2, 3, 4);

        List<List<Integer>> fixed = samples.stream().gather(Gatherers.windowFixed(2)).toList();
        List<List<Integer>> sliding = samples.stream().gather(Gatherers.windowSliding(2)).toList();
        List<Integer> scanned = samples.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList();
        List<Integer> folded = samples.stream().gather(Gatherers.fold(() -> 0, Integer::sum)).toList();

        System.out.println(fixed); // [[1, 2], [3, 4]]  — non-overlapping batches
        System.out.println(sliding); // [[1, 2], [2, 3], [3, 4]] — overlapping windows
        System.out.println(scanned); // [1, 3, 6, 10] — running total after each element
        System.out.println(folded); // [10] — one final value
    }
}
