package lab.concurrency.unboundedthreads;

public class ReportGenerationWorker {
    public void generateReport(String reportId) {
        if (reportId == null || reportId.isBlank()) {
            throw new IllegalArgumentException("reportId must not be blank");
        }
        // Simulated CPU and I/O report generation
    }
}
