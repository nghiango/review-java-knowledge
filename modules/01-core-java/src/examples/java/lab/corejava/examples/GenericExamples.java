package lab.corejava.examples;

import java.util.ArrayList;
import java.util.List;

public final class GenericExamples {
    private GenericExamples() {}

    public static <T> void copy(List<? extends T> source, List<? super T> target) {
        target.addAll(source);
    }

    public static List<Number> copyIntegersToNumbers() {
        List<Integer> source = List.of(1, 2, 3);
        List<Number> target = new ArrayList<>();
        copy(source, target);
        return List.copyOf(target);
    }

    public static boolean sameRuntimeClassAfterErasure() {
        return new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass();
    }
}
