package lab.kafka.broken.blockingpoll;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ReportGenerationListener {

    private final HeavyReportGenerator reportGenerator;
    private final ReportStorageClient storageClient;

    public ReportGenerationListener(
            HeavyReportGenerator reportGenerator,
            ReportStorageClient storageClient) {
        this.reportGenerator = reportGenerator;
        this.storageClient = storageClient;
    }

    // Default max.poll.interval.ms is 300,000ms (5 minutes).
    // Processing an entire complex business report synchronously on the Kafka listener thread
    // causes the listener to exceed max.poll.interval.ms.
    @KafkaListener(
            topics = "report-generation-tasks",
            groupId = "report-generator-group")
    public void onReportTask(ReportTask task, Acknowledgment ack) {
        byte[] pdfReport = reportGenerator.generateBigReport(task.reportType(), task.parameters());
        storageClient.uploadReport(task.taskId(), pdfReport);

        if (ack != null) {
            ack.acknowledge();
        }
    }

    public interface HeavyReportGenerator {
        byte[] generateBigReport(String reportType, java.util.Map<String, String> parameters);
    }

    public interface ReportStorageClient {
        void uploadReport(String taskId, byte[] content);
    }
}
