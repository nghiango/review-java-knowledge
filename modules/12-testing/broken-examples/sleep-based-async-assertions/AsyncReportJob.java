package lab.testing.broken.sleepbasedasyncassertions;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lab.testing.async.ReportStatus;

/**
 * Runs report generation on a background thread and tracks the status of each report.
 *
 * <p>Kept deliberately small: this class accepts a report, runs the simulated generation on the
 * injected executor and records the outcome, so the companion test can observe the transition.
 */
public final class AsyncReportJob implements AutoCloseable {

    private final ExecutorService executor;
    private final Duration simulatedWork;
    private final Map<String, ReportStatus> statuses = new ConcurrentHashMap<>();

    public AsyncReportJob(ExecutorService executor, Duration simulatedWork) {
        this.executor = executor;
        this.simulatedWork = simulatedWork;
    }

    public ReportStatus submit(String reportId) {
        statuses.put(reportId, ReportStatus.QUEUED);
        executor.execute(() -> generate(reportId));
        return ReportStatus.QUEUED;
    }

    public ReportStatus status(String reportId) {
        return statuses.get(reportId);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void generate(String reportId) {
        statuses.put(reportId, ReportStatus.RUNNING);
        try {
            CountDownLatch workGate = new CountDownLatch(1);
            workGate.await(simulatedWork.toMillis(), TimeUnit.MILLISECONDS);
            statuses.put(reportId, ReportStatus.COMPLETED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            statuses.put(reportId, ReportStatus.FAILED);
        }
    }
}
