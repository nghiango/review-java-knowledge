package lab.jvm.questions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

@SuppressWarnings("unused")
public final class Q30NativeMemoryExhaustionScenarioExample {
    private Q30NativeMemoryExhaustionScenarioExample() {}

    public static void main(String[] args) {
        // java.util.zip.Deflater uses native zlib C libraries.
        // It allocates native memory via malloc for its compression dictionary buffer.
        // If deflater.end() is omitted, the native memory remains allocated until the Cleaner runs.
        List<Deflater> deflaters = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Deflater deflater = new Deflater();
            byte[] input = "payload chunk".getBytes(StandardCharsets.UTF_8);
            deflater.setInput(input);
            deflater.finish();
            deflaters.add(deflater);
            // Missing: deflater.end();
        }

        // Proper deterministic cleanup:
        for (Deflater d : deflaters) {
            d.end(); // explicitly frees C zlib native buffers via unmap/free
        }

        boolean finished = deflaters.getFirst().finished(); // true
    }
}
