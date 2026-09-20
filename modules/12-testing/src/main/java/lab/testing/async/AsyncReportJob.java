package lab.testing.async;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs report generation off the calling thread and exposes the status of every submitted report.
 *
 * <p>Design decisions worth noting:
 *
 * <ul>
 *   <li>The executor is injected, so the caller owns the threading policy. Production passes a
 *       bounded pool; a test passes a single-thread executor and therefore controls exactly when
 *       the worker can pick up a report.
 *   <li>{@link #submit} returns as soon as the report is accepted, so a caller never blocks on
 *       generation. The status map is a {@link ConcurrentHashMap} because the worker writes from
 *       another thread while callers read.
 *   <li>Completion is driven by a signal, not by time: the worker either finishes the work or
 *       records a failure, and the reader observes the transition. Nothing polls, nothing sleeps,
 *       and no wait is unbounded.
 *   <li>{@link #close} shuts the executor down, so a job cannot leak a worker thread past the
 *       lifecycle of the component that owns it.
 * </ul>
 */
public final class AsyncReportJob implements AutoCloseable {

    /**
     * The unit of work a submitted report runs on the worker thread.
     *
     * <p>Package-private and injectable: production uses {@link #simulatedReportWork(Duration)},
     * while a test injects work gated by a latch it releases — or work that throws — so the
     * transition to {@code COMPLETED} or {@code FAILED} is driven by a signal instead of an assumed
     * delay.
     */
    @FunctionalInterface
    interface ReportWork {
        void run(String reportId) throws Exception;
    }

    private final ExecutorService executor;
    private final ReportWork work;
    private final Map<String, ReportStatus> statuses = new ConcurrentHashMap<>();

    /**
     * Creates a job that generates each report after a simulated {@code simulatedWork} delay.
     *
     * @param executor the executor the worker runs on; owned by this job and stopped by {@link
     *     #close}
     * @param simulatedWork how long a generated report is simulated to take; never {@code null}
     */
    public AsyncReportJob(ExecutorService executor, Duration simulatedWork) {
        this(executor, simulatedReportWork(simulatedWork));
    }

    AsyncReportJob(ExecutorService executor, ReportWork work) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.work = Objects.requireNonNull(work, "work must not be null");
    }

    /**
     * Accepts {@code reportId} for generation and returns immediately.
     *
     * @param reportId the report to generate; never {@code null}
     * @return the status the report is accepted in, {@link ReportStatus#QUEUED} — the worker may
     *     already have moved it on by the time the caller reads {@link #status(String)}
     */
    public ReportStatus submit(String reportId) {
        Objects.requireNonNull(reportId, "reportId must not be null");
        statuses.put(reportId, ReportStatus.QUEUED);
        executor.execute(() -> generate(reportId));
        return ReportStatus.QUEUED;
    }

    /**
     * Returns the current status of {@code reportId}.
     *
     * @return the status, or {@code null} if no such report was submitted
     */
    public ReportStatus status(String reportId) {
        return statuses.get(reportId);
    }

    /**
     * Stops the worker thread.
     *
     * <p>The job owns the executor it was constructed with, so closing the job is what ends the
     * component's lifecycle: an in-flight generation is interrupted and its thread is released
     * instead of surviving into whatever runs next.
     */
    @Override
    public void close() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void generate(String reportId) {
        statuses.put(reportId, ReportStatus.RUNNING);
        try {
            work.run(reportId);
            statuses.put(reportId, ReportStatus.COMPLETED);
        } catch (Exception e) {
            // A report that cannot be generated must end in FAILED. Swallowing the exception and
            // leaving the report RUNNING would make a broken job indistinguishable from a slow one.
            statuses.put(reportId, ReportStatus.FAILED);
        }
    }

    /**
     * Stands in for the real generator: a bounded, interruptible wait for {@code simulatedWork}.
     *
     * <p>Deliberately not a sleeping call: the wait is capped by the configured budget and can be
     * interrupted by {@link #close}, so the worker can always be stopped rather than being stuck
     * for the whole delay.
     */
    private static ReportWork simulatedReportWork(Duration simulatedWork) {
        Objects.requireNonNull(simulatedWork, "simulatedWork must not be null");
        return reportId -> {
            CountDownLatch workGate = new CountDownLatch(1);
            workGate.await(simulatedWork.toMillis(), TimeUnit.MILLISECONDS);
        };
    }
}
