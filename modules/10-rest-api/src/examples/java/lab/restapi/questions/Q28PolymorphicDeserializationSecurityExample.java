package lab.restapi.questions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public final class Q28PolymorphicDeserializationSecurityExample {
    private Q28PolymorphicDeserializationSecurityExample() {}

    // INSECURE VULNERABILITY (CVE-2017-7525 gadget chains):
    // @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) allows arbitrary class names in JSON payloads!
    // An attacker sends {"@class":"org.springframework.context.support.FileSystemXmlApplicationContext", ...}
    // executing arbitrary remote code during deserialization!

    // SECURE PATTERN: Logical type name discriminator with strict explicit whitelist:
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
    )
    @JsonSubTypes({
        @JsonSubTypes.Type(value = EmailNotification.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = SmsNotification.class, name = "SMS")
    })
    public sealed interface Notification permits EmailNotification, SmsNotification {
        String recipient();
    }

    public record EmailNotification(String recipient, String subject) implements Notification {}
    public record SmsNotification(String recipient, String message) implements Notification {}

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"type\":\"EMAIL\",\"recipient\":\"alice@example.com\",\"subject\":\"Welcome\"}";

        Notification notification = mapper.readValue(json, Notification.class);
        boolean isEmail = notification instanceof EmailNotification; // true (safely deserialized via whitelist)
    }
}
