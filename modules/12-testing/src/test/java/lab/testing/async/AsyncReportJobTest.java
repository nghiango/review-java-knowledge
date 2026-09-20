package lab.testing.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link AsyncReportJob}.
 *
 * <p>Every test drives the job with a single-thread executor and, where the transition matters,
 * with work the test releases itself. Completion is observed by polling the status the test asserts
 * on with Awaitility — never by waiting a fixed delay — so the class passes on a fast laptop, on a
 * loaded CI box, and when the work throws.
 */
class AsyncReportJobTest {

    private static final Duration WORK_BUDGET = Duration.ofSeconds(2);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(25);

    private ExecutorService executor;
    private AsyncReportJob job;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        job = new AsyncReportJob(executor, Duration.ofMillis(20));
    }

    @AfterEach
    void tearDown() {
        // close() shuts the executor down, so no worker thread survives into the next test.
        job.close();
    }

    @Test
    @DisplayName("submit accepts a report in QUEUED state")
    void submit_newReport_isQueued() {
        assertThat(job.submit("RPT-1")).isEqualTo(ReportStatus.QUEUED);
    }

    @Test
    @DisplayName(
            "a submitted report runs asynchronously and reaches COMPLETED once its work is released")
    void submit_reportWithGatedWork_runsThenCompletes() {
        CountDownLatch release = new CountDownLatch(1);
        job = new AsyncReportJob(executor, reportId -> release.await());

        job.submit("RPT-1");

        await().atMost(WORK_BUDGET)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(
                        () -> assertThat(job.status("RPT-1")).isEqualTo(ReportStatus.RUNNING));

        release.countDown();

        await().atMost(WORK_BUDGET)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(
                        () -> assertThat(job.status("RPT-1")).isEqualTo(ReportStatus.COMPLETED));
    }

    @Test
    @DisplayName("a report whose work throws reaches FAILED instead of staying RUNNING")
    void submit_failingWork_reachesFailed() {
        job =
                new AsyncReportJob(
                        executor,
                        reportId -> {
                            throw new IllegalStateException("generator unavailable");
                        });

        job.submit("RPT-2");

        await().atMost(WORK_BUDGET)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(
                        () -> assertThat(job.status("RPT-2")).isEqualTo(ReportStatus.FAILED));
    }

    @Test
    @DisplayName("the simulated work of the default constructor completes on its own")
    void submit_simulatedWork_reachesCompleted() {
        job.submit("RPT-3");

        await().atMost(WORK_BUDGET)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(
                        () -> assertThat(job.status("RPT-3")).isEqualTo(ReportStatus.COMPLETED));
    }

    @Test
    @DisplayName("a null report id is rejected instead of being accepted silently")
    void submit_nullReportId_rejected() {
        assertThatThrownBy(() -> job.submit(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("close stops the executor so no worker thread survives the job")
    void close_stopsWorkerThread() {
        job.close();

        assertThat(executor.isShutdown()).isTrue();
    }
}
