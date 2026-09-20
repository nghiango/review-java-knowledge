package lab.kafka.asyncpoll;

import java.util.Map;

public record ReportTask(
        String taskId, String reportType, String requestedBy, Map<String, String> parameters) {}
