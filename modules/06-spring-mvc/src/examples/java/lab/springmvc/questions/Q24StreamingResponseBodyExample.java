package lab.springmvc.questions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@SuppressWarnings("unused")
public final class Q24StreamingResponseBodyExample {
    private Q24StreamingResponseBodyExample() {}

    // StreamingResponseBody allows writing directly to the client's HTTP response OutputStream
    // asynchronously on an application task thread, without buffering the entire payload in heap memory.
    public static StreamingResponseBody streamReportData() {
        return (OutputStream outputStream) -> {
            for (int i = 1; i <= 3; i++) {
                String chunk = "chunk-" + i + "\n";
                outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }
        };
    }

    public static void main(String[] args) throws IOException {
        StreamingResponseBody body = streamReportData();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        body.writeTo(buffer);

        String result = buffer.toString(StandardCharsets.UTF_8);
        boolean containsChunks = result.contains("chunk-1") && result.contains("chunk-3"); // true
    }
}
