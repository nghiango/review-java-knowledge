package lab.java25boot4.whatsnew.broken.propertybinding;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binding for the outbound notification pipeline. The property names were carried over unchanged
 * from the Spring Boot 3.5 service during the upgrade.
 */
@ConfigurationProperties(prefix = "notifications.outbound")
public class LegacyMessagingSettings {

    private String endpoint;
    private String topic;
    private int batchSize;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
