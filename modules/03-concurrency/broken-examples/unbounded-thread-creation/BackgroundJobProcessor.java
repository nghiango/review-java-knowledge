package lab.concurrency.broken.unboundedthreads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundJobProcessor {
    private final ReportGenerationWorker worker;
    // Cached thread pool allows up to Integer.MAX_VALUE threads with SynchronousQueue
    private final ExecutorService cachedPool = Executors.newCachedThreadPool();

    public BackgroundJobProcessor(ReportGenerationWorker worker) {
        this.worker = worker;
    }

    public void processStandardJob(String reportId) {
        cachedPool.submit(() -> worker.generateReport(reportId));
    }

    public void processUrgentJob(String reportId) {
        // Spawns an unmanaged native OS thread directly per request without lifecycle tracking
        new Thread(() -> worker.generateReport(reportId)).start();
    }

    public void stopImmediately() {
        // Hard shutdownNow without awaiting termination drops queued tasks and interrupts running ones
        cachedPool.shutdownNow();
    }
}
