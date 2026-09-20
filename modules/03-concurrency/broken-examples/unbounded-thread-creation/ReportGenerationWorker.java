package lab.concurrency.broken.unboundedthreads;

public class ReportGenerationWorker {
    public void generateReport(String reportId) {
        try {
            // Simulated CPU and I/O report rendering
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
