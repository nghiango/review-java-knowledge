package lab.performance.broken.allocation;

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MetricLineEncoder {
    private static final Logger LOGGER = Logger.getLogger(MetricLineEncoder.class.getName());

    public String encode(String name, Map<String, String> tags, long value) {
        Pattern invalid = Pattern.compile("[^a-zA-Z0-9_.-]");
        String normalized = invalid.matcher(name).replaceAll("_");
        Map<String, String> escaped =
                tags.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().replace(",", "\\,")));
        String encodedTags =
                escaped.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(","));
        String line = String.format("%s{%s}=%d", normalized, encodedTags, value);
        LOGGER.info(line);
        return line;
    }
}
