package lab.java25boot4.springmvc.questions;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class Q13ScenarioApiVersionDeprecationSunsetExample {

    public static void main(String[] args) {
        // Incident mitigation: Deprecating v1 while serving v2.
        // Standard RFC 8594 Sunset and Deprecation headers notify API consumers ahead of shutdown.
        String sunsetHeader =
                DateTimeFormatter.ISO_INSTANT.format(Instant.parse("2027-01-01T00:00:00Z"));
        System.out.println("Sunset: " + sunsetHeader); // Sunset: 2027-01-01T00:00:00Z
        System.out.println("Deprecation: true"); // Deprecation: true
    }
}
