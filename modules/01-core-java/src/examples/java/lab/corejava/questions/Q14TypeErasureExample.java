package lab.corejava.questions;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q14TypeErasureExample {
    private Q14TypeErasureExample() {}

    public static void main(String[] args) {
        List<String> stringList = new ArrayList<>();
        List<Integer> intList = new ArrayList<>();

        // Generic type parameters <String> and <Integer> are erased at runtime to raw List / Object
        boolean sameClass =
                (stringList.getClass()
                        == intList.getClass()); // true (both are java.util.ArrayList)
        String typeName = stringList.getClass().getName(); // "java.util.ArrayList"
    }
}
