package lab.springmvc.broken.asyncprocessing;

import org.springframework.stereotype.Service;

@Service
public class ReportGenerationService {

    public String generateHeavyReport(String datasetId) {
        long sum = 0;
        for (int i = 0; i < 1_000_000; i++) {
            sum += (long) i * datasetId.hashCode();
        }
        return "Report generated for " + datasetId + ", checksum=" + sum;
    }
}
