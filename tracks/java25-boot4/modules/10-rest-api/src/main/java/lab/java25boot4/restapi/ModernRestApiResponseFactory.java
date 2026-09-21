package lab.java25boot4.restapi;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Production response factory constructing RFC 9457 Problem Details and RFC 8594 lifecycle headers.
 */
public final class ModernRestApiResponseFactory {

    private static final DateTimeFormatter HTTP_DATE_FORMATTER =
            DateTimeFormatter.RFC_1123_DATE_TIME;

    private ModernRestApiResponseFactory() {}

    /**
     * Constructs a RFC 8594 compliant HttpHeaders instance indicating API deprecation and sunset.
     */
    public static HttpHeaders createLifecycleHeaders(
            ZonedDateTime deprecationTime, ZonedDateTime sunsetTime, String migrationGuideUrl) {
        HttpHeaders headers = new HttpHeaders();
        // RFC 8594 §2: Deprecation header with @epoch-seconds or true
        headers.set("Deprecation", "@" + deprecationTime.toEpochSecond());
        // RFC 8594 §3: Sunset header with IMF-fixdate (RFC 7231 format)
        headers.set(
                "Sunset",
                HTTP_DATE_FORMATTER.format(sunsetTime.withZoneSameInstant(ZoneOffset.UTC)));
        // RFC 8594 §4: Link header pointing to deprecation / migration documentation
        headers.set(HttpHeaders.LINK, "<" + migrationGuideUrl + ">; rel=\"deprecation\"");
        return headers;
    }

    /** Constructs an RFC 9457 ProblemDetail response for unsupported API version requests. */
    public static ProblemDetail createUnsupportedVersionProblem(
            String requestedVersion, List<String> supportedVersions) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "The requested API version '"
                                + requestedVersion
                                + "' is not supported by this service.");
        problem.setTitle("Unsupported API Version");
        problem.setType(URI.create("https://api.example.com/errors/unsupported-version"));
        problem.setProperty("requestedVersion", requestedVersion);
        problem.setProperty("supportedVersions", supportedVersions);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    /** Constructs an RFC 9457 ProblemDetail response from downstream service errors. */
    public static ProblemDetail createRemoteDownstreamProblem(
            HttpStatus status, String remoteService, String detail, String traceId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle("Remote Service Dependency Error");
        problem.setType(URI.create("https://api.example.com/errors/downstream-failure"));
        problem.setProperty("remoteService", remoteService);
        problem.setProperty("traceId", traceId);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }
}
