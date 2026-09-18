package lab.corejava.questions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public final class Q21ResourceSafetyAndAtomicityExample {
    private Q21ResourceSafetyAndAtomicityExample() {}

    public static List<String> parseAllOrNothing(String csvContent) throws IOException {
        List<String> tempBuffer = new ArrayList<>();
        // try-with-resources guarantees reader closure regardless of parse exceptions
        try (var reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("CORRUPTED")) {
                    throw new IllegalArgumentException("Invalid row format");
                }
                tempBuffer.add(line);
            }
        }
        return List.copyOf(tempBuffer); // atomicity: only committed upon complete successful parse
    }

    public static void main(String[] args) throws IOException {
        List<String> validRows = parseAllOrNothing("row1\nrow2");
        int count = validRows.size(); // 2
    }
}
