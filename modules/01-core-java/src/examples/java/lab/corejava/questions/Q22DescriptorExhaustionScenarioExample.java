package lab.corejava.questions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

@SuppressWarnings("unused")
public final class Q22DescriptorExhaustionScenarioExample {
    private Q22DescriptorExhaustionScenarioExample() {}

    public static String safeReadSingleLine(String data) throws IOException {
        // Deterministic try-with-resources closes OS file/stream descriptor immediately
        try (var reader = new BufferedReader(new StringReader(data))) {
            return reader.readLine(); // "payload line"
        }
    }

    public static void main(String[] args) throws IOException {
        String result =
                safeReadSingleLine("payload line"); // "payload line" (descriptor safely released)
    }
}
