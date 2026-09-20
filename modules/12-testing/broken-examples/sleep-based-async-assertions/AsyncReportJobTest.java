package lab.testing.broken.sleepbasedasyncassertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lab.testing.async.ReportStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncReportJob}.
 *
 * <p>Generating a report is simulated to take half a second, so each test gives the background
 * thread time to finish before it checks the status it expects.
 */
class AsyncReportJobTest {

    private static final Duration REPORT_GENERATION = Duration.ofMillis(500);

    private ExecutorService executor;
    private AsyncReportJob job;

    @BeforeEach
    void setUp() {
        executor = Executors.newSingleThreadExecutor();
        job = new AsyncReportJob(executor, REPORT_GENERATION);
    }

    @AfterEach
    void tearDown() {
        job.close();
    }

    @Test
    void submit_thenWait_statusIsCompleted() throws InterruptedException {
        job.submit("RPT-1");

        Thread.sleep(500);

        assertEquals(ReportStatus.COMPLETED, job.status("RPT-1"));
    }

    @Test
    void submit_secondReport_statusIsCompleted() throws InterruptedException {
        job.submit("RPT-2");

        while (job.status("RPT-2") != ReportStatus.COMPLETED) {
            Thread.sleep(50);
        }

        assertEquals(ReportStatus.COMPLETED, job.status("RPT-2"));
    }
}
