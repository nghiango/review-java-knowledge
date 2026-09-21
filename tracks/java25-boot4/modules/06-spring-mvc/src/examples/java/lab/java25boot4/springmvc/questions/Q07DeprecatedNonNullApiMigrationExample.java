package lab.java25boot4.springmvc.questions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Q07DeprecatedNonNullApiMigrationExample {

    // Spring Framework 7 deprecates @NonNullApi in favor of JSpecify @NullMarked
    public static @Nullable String legacyLookup(String key) {
        if ("known".equals(key)) {
            return "Value";
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println(legacyLookup("known")); // Value
        System.out.println(legacyLookup("unknown")); // null
    }
}
