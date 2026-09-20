# Solution — Blocking Listener Thread Exceeding Max Poll Interval

## Annotated code

```java
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

    @KafkaListener(
            topics = "report-generation-tasks",
            groupId = "report-generator-group")
    public void onReportTask(ReportTask task, Acknowledgment ack) {
        // Reliability issue: Executing heavy CPU/IO processing directly on the Kafka listener thread.
        // If generateBigReport() takes longer than max.poll.interval.ms (default 300,000ms / 5 minutes),
        // the consumer coordinator considers the worker dead, revokes its partition assignment,
        // and triggers a group rebalance.
        byte[] pdfReport = reportGenerator.generateBigReport(task.reportType(), task.parameters());
        storageClient.uploadReport(task.taskId(), pdfReport);

        // Reliability issue: When this acknowledgment is attempted after a rebalance,
        // the consumer coordinator rejects it with a CommitFailedException because the partition
        // was reassigned to another consumer instance, which will reprocess the same task from scratch.
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
```

## Issue list

### Reliability issue: Listener execution exceeds `max.poll.interval.ms` triggering rebalance storm

- **Location:** `ReportGenerationListener.java:23-28`
- **Description:** Long-running CPU/IO rendering (8-10 minutes) executes synchronously on the listener thread, preventing the Kafka consumer loop from calling `poll()`.
- **Impact:** In Kafka, consumer heartbeats run in a background thread, but `max.poll.interval.ms` monitors the foreground application thread. If consecutive calls to `poll()` take longer than `max.poll.interval.ms` (default 5 minutes), the group coordinator assumes the worker has hung, marks it dead, and initiates a consumer group rebalance. The worker's assigned partitions are reassigned to peer consumers. When the original worker finally finishes and calls `ack.acknowledge()`, Kafka throws `CommitFailedException`. Meanwhile, the new consumer begins processing the exact same heavy report from the uncommitted offset, causing a catastrophic cascade of recurring rebalance storms and duplicate reports across the entire cluster.
- **Remediation:** 
  1. Offload the heavy task to a separate asynchronous worker pool (e.g. `CompletableFuture` or `ThreadPoolTaskExecutor`) and pause the consumer partition via `ConsumerSeekAware` or `consumer.pause()` until completion, or
  2. Increase `max.poll.interval.ms` and decrease `max.poll.records` to ensure the consumer returns within the poll interval deadline, or
  3. Decouple reporting into an asynchronous batch job pattern where the Kafka event initiates a lightweight DB record/job submission and returns immediately.
