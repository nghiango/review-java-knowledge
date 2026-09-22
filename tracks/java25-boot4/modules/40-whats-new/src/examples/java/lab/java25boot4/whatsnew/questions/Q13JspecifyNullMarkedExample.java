package lab.java25boot4.whatsnew.questions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Q13: how does JSpecify {@code @NullMarked} change a public API contract? */
@NullMarked
public class Q13JspecifyNullMarkedExample {

    // Inside a @NullMarked scope every reference is non-null unless it is marked @Nullable.
    static String requireName(@Nullable String name) {
        return name == null ? "unknown" : name; // the @Nullable marks absence as part of the contract
    }

    public static void main(String[] args) {
        System.out.println(requireName(null)); // unknown
        System.out.println(requireName("acme")); // acme
    }
}
