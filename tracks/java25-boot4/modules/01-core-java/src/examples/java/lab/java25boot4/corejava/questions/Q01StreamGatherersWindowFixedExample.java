package lab.java25boot4.corejava.questions;

import java.util.List;
import java.util.stream.Gatherers;

public class Q01StreamGatherersWindowFixedExample {

    public static void main(String[] args) {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7);

        // Gatherers.windowFixed groups elements into fixed-size lists
        List<List<Integer>> windows = numbers.stream().gather(Gatherers.windowFixed(3)).toList();

        System.out.println(windows.size()); // 3
        System.out.println(windows.get(0)); // [1, 2, 3]
        System.out.println(windows.get(1)); // [4, 5, 6]
        System.out.println(windows.get(2)); // [7]
    }
}
