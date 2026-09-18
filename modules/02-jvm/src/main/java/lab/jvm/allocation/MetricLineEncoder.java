package lab.jvm.allocation;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class MetricLineEncoder {
    private MetricLineEncoder() {}

    public static String encode(MetricEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Map<String, String> tags = event.tags();

        if (tags.isEmpty()) {
            return event.name() + "=" + event.value();
        }

        StringBuilder buffer = new StringBuilder(64);
        buffer.append(event.name()).append('{');

        if (tags.size() == 1) {
            Map.Entry<String, String> entry = tags.entrySet().iterator().next();
            appendEscaped(buffer, entry.getKey());
            buffer.append('=');
            appendEscaped(buffer, entry.getValue());
        } else {
            String[] keys = tags.keySet().toArray(new String[0]);
            Arrays.sort(keys);
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    buffer.append(',');
                }
                String key = keys[i];
                appendEscaped(buffer, key);
                buffer.append('=');
                appendEscaped(buffer, tags.get(key));
            }
        }

        buffer.append("}=").append(event.value());
        return buffer.toString();
    }

    private static void appendEscaped(StringBuilder target, String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\' || ch == ',' || ch == '=') {
                target.append('\\');
            }
            target.append(ch);
        }
    }
}
