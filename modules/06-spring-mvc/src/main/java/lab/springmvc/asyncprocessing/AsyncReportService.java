package lab.springmvc.asyncprocessing;

import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

@Service
public class AsyncReportService {

    public CompletableFuture<String> generateHeavyReportAsync(String datasetId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    long sum = 0;
                    for (int i = 0; i < 10_000; i++) {
                        sum += (long) i * datasetId.hashCode();
                    }
                    return "Report generated for " + datasetId + ", checksum=" + sum;
                });
    }
}
