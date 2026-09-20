package lab.concurrency.unboundedthreads;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production-ready job processor that enforces queue and pool bounds and guarantees reliable
 * two-phase graceful shutdown.
 */
public class BoundedJobProcessor implements AutoCloseable {
    private final ReportGenerationWorker worker;
    private final ThreadPoolExecutor executor;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public BoundedJobProcessor(ReportGenerationWorker worker, ThreadPoolExecutor executor) {
        this.worker = Objects.requireNonNull(worker, "worker must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    public Future<?> submitJob(String reportId) {
        if (!running.get()) {
            throw new IllegalStateException("Processor has been shut down");
        }
        try {
            return executor.submit(() -> worker.generateReport(reportId));
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException("Job rejected: worker queue capacity exceeded", e);
        }
    }

    public int getQueueDepth() {
        return executor.getQueue().size();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public boolean gracefulShutdown(long timeout, TimeUnit unit) throws InterruptedException {
        running.set(false);
        executor.shutdown(); // Phase 1: Disable new tasks
        boolean completed = executor.awaitTermination(timeout, unit);
        if (!completed) {
            executor.shutdownNow(); // Phase 2: Cancel currently executing tasks
            completed = executor.awaitTermination(timeout, unit);
        }
        return completed;
    }

    @Override
    public void close() throws Exception {
        gracefulShutdown(5, TimeUnit.SECONDS);
    }
}
