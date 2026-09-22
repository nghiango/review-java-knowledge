package lab.java25boot4.whatsnew.questions;

import java.util.Map;

/** Q18: a feature silently stops working after the upgrade — how do you find the cause? */
public class Q18RemovedPropertyDiagnosisExample {

    public static void main(String[] args) {
        // The application.yml still uses the Boot 3.5 key; the Boot 4 binding looks for a new key.
        Map<String, String> applicationYml = Map.of("notifications.outbound.destination", "orders");
        String boundKey = "notifications.outbound.topic";

        String boundValue = applicationYml.get(boundKey);

        System.out.println(boundValue); // null — the key exists under a name nothing binds any more
        System.out.println(applicationYml.containsKey(boundKey)); // false — no error, just no match
        // Detection: run with --debug and read the configuration-properties binding report.
    }
}
