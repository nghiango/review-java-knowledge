package lab.kafka.asyncpoll;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Production-grade listener preventing {@code max.poll.interval.ms} rebalance timeouts.
 *
 * <p>Offloads long-running heavy report generation to a dedicated bounded background executor. The
 * Kafka poll thread returns promptly to poll the broker, maintaining healthy consumer group
 * membership and eliminating rebalance storms.
 */
@Component
public class SafeReportGenerationListener {

    public static final String TOPIC = "report-generation-tasks";
    public static final String GROUP_ID = "report-generator-group";

    private final HeavyReportGenerator reportGenerator;
    private final ReportStorageClient storageClient;
    private final Executor reportWorkerExecutor;

    public SafeReportGenerationListener(
            HeavyReportGenerator reportGenerator,
            ReportStorageClient storageClient,
            Executor reportWorkerExecutor) {
        this.reportGenerator = reportGenerator;
        this.storageClient = storageClient;
        this.reportWorkerExecutor = reportWorkerExecutor;
    }

    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public CompletableFuture<Void> onReportTask(ReportTask task, Acknowledgment ack) {
        // Offload heavy processing to bounded background worker thread
        return CompletableFuture.runAsync(
                        () -> {
                            byte[] report =
                                    reportGenerator.generateBigReport(
                                            task.reportType(), task.parameters());
                            storageClient.uploadReport(task.taskId(), report);
                        },
                        reportWorkerExecutor)
                .thenRun(
                        () -> {
                            if (ack != null) {
                                ack.acknowledge();
                            }
                        })
                .exceptionally(
                        ex -> {
                            storageClient.recordFailure(task.taskId(), ex.getMessage());
                            return null;
                        });
    }

    public interface HeavyReportGenerator {
        byte[] generateBigReport(String reportType, Map<String, String> parameters);
    }

    public interface ReportStorageClient {
        void uploadReport(String taskId, byte[] content);

        void recordFailure(String taskId, String reason);
    }
}
