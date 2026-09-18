package lab.corejava.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

public final class ResourceExamples {
    private ResourceExamples() {}

    public static String firstLine(String content) throws IOException {
        try (var reader = new BufferedReader(new StringReader(content))) {
            return reader.readLine();
        }
    }

    public static String requireProfile(Optional<String> profile) {
        return profile.orElseThrow(() -> new IllegalArgumentException("profile missing"));
    }

    public static IOException bodyAndCloseFailure() {
        var reader =
                new BufferedReader(new StringReader("unused")) {
                    @Override
                    public void close() throws IOException {
                        throw new IOException("close failed");
                    }
                };
        try (reader) {
            throw new IOException("read failed");
        } catch (IOException failure) {
            return failure;
        }
    }
}
