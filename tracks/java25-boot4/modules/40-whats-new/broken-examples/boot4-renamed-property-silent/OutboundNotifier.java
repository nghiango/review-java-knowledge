package lab.java25boot4.whatsnew.broken.propertybinding;

/**
 * Publishes outbound notifications using the bound pipeline settings.
 */
public class OutboundNotifier {

    private final LegacyMessagingSettings settings;

    public OutboundNotifier(LegacyMessagingSettings settings) {
        this.settings = settings;
    }

    public String publish(String payload) {
        if (settings.getTopic() == null) {
            return "skipped";
        }
        return "published "
                + payload
                + " to "
                + settings.getTopic()
                + " (batch="
                + settings.getBatchSize()
                + ")";
    }
}
