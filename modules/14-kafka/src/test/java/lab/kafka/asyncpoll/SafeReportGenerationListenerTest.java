package lab.kafka.asyncpoll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class SafeReportGenerationListenerTest {

    @Mock private SafeReportGenerationListener.HeavyReportGenerator reportGenerator;

    @Mock private SafeReportGenerationListener.ReportStorageClient storageClient;

    @Mock private Acknowledgment acknowledgment;

    // Direct synchronous executor for deterministic unit test assertions
    private final Executor directExecutor = Runnable::run;

    @Test
    @DisplayName("Offloads report generation to async executor and acknowledges after completion")
    void onReportTask_success_generatesUploadsAndAcknowledges() {
        SafeReportGenerationListener listener =
                new SafeReportGenerationListener(reportGenerator, storageClient, directExecutor);

        ReportTask task =
                new ReportTask("task-1", "QUARTERLY_SALES", "user-1", Map.of("quarter", "Q4"));
        byte[] fakePdf = new byte[] {1, 2, 3};

        when(reportGenerator.generateBigReport("QUARTERLY_SALES", Map.of("quarter", "Q4")))
                .thenReturn(fakePdf);

        CompletableFuture<Void> future = listener.onReportTask(task, acknowledgment);
        future.join();

        assertThat(future).isCompleted();
        verify(storageClient).uploadReport("task-1", fakePdf);
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Handles background generation failure without uncaught thread leak")
    void onReportTask_failure_recordsError() {
        SafeReportGenerationListener listener =
                new SafeReportGenerationListener(reportGenerator, storageClient, directExecutor);

        ReportTask task = new ReportTask("task-2", "TAX_AUDIT", "user-2", Map.of());

        when(reportGenerator.generateBigReport(any(), any()))
                .thenThrow(new RuntimeException("Data store connection failure"));

        CompletableFuture<Void> future = listener.onReportTask(task, acknowledgment);
        future.join();

        verify(storageClient)
                .recordFailure(eq("task-2"), contains("Data store connection failure"));
        verify(acknowledgment, never()).acknowledge();
    }
}
