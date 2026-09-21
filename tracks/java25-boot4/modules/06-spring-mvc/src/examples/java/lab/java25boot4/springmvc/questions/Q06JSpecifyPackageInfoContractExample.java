package lab.java25boot4.springmvc.questions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Q06JSpecifyPackageInfoContractExample {

    public static String computeLength(String input, @Nullable String fallback) {
        if (input.isEmpty() && fallback != null) {
            return fallback;
        }
        return input;
    }

    public static void main(String[] args) {
        System.out.println(computeLength("hello", null)); // hello
        System.out.println(computeLength("", "fallback")); // fallback
    }
}
