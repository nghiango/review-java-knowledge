package lab.observability.questions;

import java.util.regex.Pattern;

public class Q16LogbackMaskingPatternExample {

    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}(\\d{4})\\b");

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // Logback masking layouts use custom CompositeConverter or PatternLayout rules
        // to regex-replace sensitive patterns before formatting output bytes.
        String rawLogMessage = "Processing transaction with card 4111-2222-3333-4444 successfully";

        String maskedMessage = CARD_PATTERN.matcher(rawLogMessage).replaceAll("****-****-****-$1");

        boolean cardMasked = maskedMessage.contains("****-****-****-4444"); // true
        boolean rawPanRemoved = !maskedMessage.contains("4111-2222-3333"); // true

        System.out.println("Masked log message: " + maskedMessage);
        System.out.println("Sensitive card PAN masked: " + cardMasked);
    }
}
