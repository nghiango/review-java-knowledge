package lab.rabbitmq.broken.unboundedprefetch;

import java.util.Map;

public record TranscodingJob(
        String jobId,
        String sourceVideoUrl,
        String targetFormat,
        Map<String, String> encodingParams) {}
