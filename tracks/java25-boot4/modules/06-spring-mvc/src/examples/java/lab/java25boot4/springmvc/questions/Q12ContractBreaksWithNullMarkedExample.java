package lab.java25boot4.springmvc.questions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Q12ContractBreaksWithNullMarkedExample {

    // When upgrading to @NullMarked, returning null without @Nullable breaches the interface
    // contract
    public static @Nullable String findOptionalDescription(boolean exists) {
        if (exists) {
            return "Item Description";
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(findOptionalDescription(true)); // Item Description
        System.out.println(findOptionalDescription(false)); // null
    }
}
