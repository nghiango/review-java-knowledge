package lab.corejava.questions;

import java.io.IOException;

@SuppressWarnings("unused")
public final class Q16SuppressedExceptionsExample {
    private Q16SuppressedExceptionsExample() {}

    public static void main(String[] args) {
        try (var resource = new FailingResource()) {
            throw new IllegalStateException("Primary business failure");
        } catch (Exception e) {
            String primaryMessage = e.getMessage(); // "Primary business failure"
            Throwable[] suppressed = e.getSuppressed();
            int suppressedCount = suppressed.length; // 1
            String suppressedMessage =
                    suppressed[0].getMessage(); // "Close failing resource failure"
        }
    }

    private static class FailingResource implements AutoCloseable {
        @Override
        public void close() throws IOException {
            throw new IOException("Close failing resource failure");
        }
    }
}
