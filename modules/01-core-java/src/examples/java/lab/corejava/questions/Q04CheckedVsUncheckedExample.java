package lab.corejava.questions;

import java.io.IOException;
import java.io.StringReader;

@SuppressWarnings("unused")
public final class Q04CheckedVsUncheckedExample {
    private Q04CheckedVsUncheckedExample() {}

    public static String readFirstChar(String input)
            throws IOException { // checked exception: must be declared/handled
        if (input == null) {
            throw new IllegalArgumentException(
                    "input cannot be null"); // unchecked exception: contract violation
        }
        try (var reader = new StringReader(input)) {
            int ch = reader.read();
            return ch == -1 ? "" : String.valueOf((char) ch); // "H"
        }
    }
}
