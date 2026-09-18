package lab.jvm.broken.excessivehotpathallocation;

import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MetricLineEncoder {
    private static final Logger LOGGER = Logger.getLogger(MetricLineEncoder.class.getName());

    public String encode(String name, Map<String, String> tags, long value) {
        if (!Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$").matcher(name).matches()) {
            throw new IllegalArgumentException("invalid name: " + name);
        }

        LOGGER.info(() -> "encoding metric for " + name);

        String formattedTags =
                tags.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(
                                entry -> {
                                    String escapedKey =
                                            entry.getKey()
                                                    .replaceAll("\\\\", "\\\\\\\\")
                                                    .replaceAll(",", "\\\\,")
                                                    .replaceAll("=", "\\\\=");
                                    String escapedVal =
                                            entry.getValue()
                                                    .replaceAll("\\\\", "\\\\\\\\")
                                                    .replaceAll(",", "\\\\,")
                                                    .replaceAll("=", "\\\\=");
                                    return String.format("%s=%s", escapedKey, escapedVal);
                                })
                        .collect(Collectors.joining(","));

        if (formattedTags.isEmpty()) {
            return String.format("%s=%d", name, value);
        }
        return String.format("%s{%s}=%d", name, formattedTags, value);
    }
}
