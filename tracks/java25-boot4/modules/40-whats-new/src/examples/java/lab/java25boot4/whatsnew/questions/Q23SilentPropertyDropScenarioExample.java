package lab.java25boot4.whatsnew.questions;

import lab.java25boot4.whatsnew.propertybinding.MessagingSettings;
import lab.java25boot4.whatsnew.propertybinding.OutboundPublisher;

/** Q23: after a Boot 4 upgrade a canary drops messages with no errors. Diagnose it. */
public class Q23SilentPropertyDropScenarioExample {

    public static void main(String[] args) {
        // With validation, a renamed key fails the deploy instead of producing null settings.
        MessagingSettings settings = new MessagingSettings("http://broker", "orders", 50);
        OutboundPublisher publisher = new OutboundPublisher(settings);

        System.out.println(publisher.publish("order-7")); // published order-7 to orders (batch=50)
        System.out.println(publisher.describeTarget()); // orders@http://broker
        // The broken version bound topic=null and returned "skipped" for every message, silently.
    }
}
