package lab.kafka.broken.blockingpoll;

import java.util.Map;

public record ReportTask(
        String taskId,
        String reportType,
        String requestedBy,
        Map<String, String> parameters) {}
