package lab.java25boot4.restapi.questions;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;

/**
 * Q03: How do RFC 8594 Sunset and Deprecation headers communicate REST endpoint lifecycle states to
 * API consumers?
 */
public class Q03Rfc8594DeprecationSunsetHeadersExample {

    public static HttpHeaders createSunsetHeaders(
            ZonedDateTime deprecationTime, ZonedDateTime sunsetTime, String doc) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Deprecation", "@" + deprecationTime.toEpochSecond());
        headers.set(
                "Sunset",
                DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        sunsetTime.withZoneSameInstant(ZoneOffset.UTC)));
        headers.set(HttpHeaders.LINK, "<" + doc + ">; rel=\"deprecation\"");
        return headers;
    }

    public static void main(String[] args) {
        ZonedDateTime deprecation = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime sunset = ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);

        HttpHeaders headers =
                createSunsetHeaders(deprecation, sunset, "https://api.example.com/docs/v2");

        System.out.println("Deprecation: " + headers.getFirst("Deprecation")); // "@1767225600"
        System.out.println(
                "Sunset: " + headers.getFirst("Sunset")); // "Thu, 31 Dec 2026 23:59:59 GMT"
        System.out.println(
                "Link: "
                        + headers.getFirst(
                                HttpHeaders.LINK)); // "<https://api.example.com/docs/v2>;
        // rel=\"deprecation\""
    }
}
