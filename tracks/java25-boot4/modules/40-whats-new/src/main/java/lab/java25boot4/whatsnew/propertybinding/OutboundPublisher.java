package lab.java25boot4.whatsnew.propertybinding;

/**
 * Publishes outbound notifications using validated settings.
 *
 * <p>Decision: no silent "skipped" branch. Validation guarantees the settings are populated, so a
 * runtime failure here is a real failure that must surface — not a normal return value that hides a
 * misconfiguration.
 */
public class OutboundPublisher {

    private final MessagingSettings settings;

    public OutboundPublisher(MessagingSettings settings) {
        this.settings = settings;
    }

    public String publish(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
        return "published "
                + payload
                + " to "
                + settings.topic()
                + " (batch="
                + settings.batchSize()
                + ")";
    }

    public String describeTarget() {
        return settings.topic() + "@" + settings.endpoint();
    }
}
