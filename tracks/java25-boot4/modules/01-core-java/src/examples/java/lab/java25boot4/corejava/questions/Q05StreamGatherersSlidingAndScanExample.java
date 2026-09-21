package lab.java25boot4.corejava.questions;

import java.util.List;
import java.util.stream.Gatherers;

public class Q05StreamGatherersSlidingAndScanExample {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // windowSliding keeps overlapping windows of a fixed size
        System.out.println(numbers.stream().gather(Gatherers.windowSliding(3)).toList());
        // [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

        // scan emits the running accumulation after every element
        System.out.println(numbers.stream().gather(Gatherers.scan(() -> 0, Integer::sum)).toList());
        // [1, 3, 6, 10, 15]

        // fold emits a single final value once the stream is exhausted
        System.out.println(numbers.stream().gather(Gatherers.fold(() -> 0, Integer::sum)).toList());
        // [15]
    }
}
