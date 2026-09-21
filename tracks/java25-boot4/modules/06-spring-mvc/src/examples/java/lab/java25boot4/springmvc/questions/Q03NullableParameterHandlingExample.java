package lab.java25boot4.springmvc.questions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Q03NullableParameterHandlingExample {

    public static String formatGreeting(String name, @Nullable String title) {
        if (title != null) {
            return "Hello, " + title + " " + name;
        }
        return "Hello, " + name;
    }

    public static void main(String[] args) {
        System.out.println(formatGreeting("Smith", "Dr.")); // Hello, Dr. Smith
        System.out.println(formatGreeting("Smith", null)); // Hello, Smith
    }
}
