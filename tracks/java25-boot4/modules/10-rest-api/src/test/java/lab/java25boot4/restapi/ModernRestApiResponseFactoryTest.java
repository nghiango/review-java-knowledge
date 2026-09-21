package lab.java25boot4.restapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ModernRestApiResponseFactoryTest {

    @Test
    @DisplayName("createLifecycleHeaders generates compliant RFC 8594 headers")
    void shouldGenerateRfc8594CompliantHeaders() {
        ZonedDateTime deprecationTime = ZonedDateTime.of(2026, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime sunsetTime = ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        String docUrl = "https://example.com/migration-v2";

        HttpHeaders headers =
                ModernRestApiResponseFactory.createLifecycleHeaders(
                        deprecationTime, sunsetTime, docUrl);

        assertThat(headers.getFirst("Deprecation"))
                .isEqualTo("@" + deprecationTime.toEpochSecond());
        assertThat(headers.getFirst("Sunset")).isEqualTo("Thu, 31 Dec 2026 23:59:59 GMT");
        assertThat(headers.getFirst(HttpHeaders.LINK))
                .isEqualTo("<https://example.com/migration-v2>; rel=\"deprecation\"");
    }

    @Test
    @DisplayName("createUnsupportedVersionProblem returns structured RFC 9457 details")
    void shouldReturnStructuredUnsupportedVersionProblem() {
        ProblemDetail problem =
                ModernRestApiResponseFactory.createUnsupportedVersionProblem(
                        "3.5", List.of("1", "2"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Unsupported API Version");
        assertThat(problem.getDetail()).contains("3.5");
        assertThat(problem.getProperties()).containsEntry("requestedVersion", "3.5");
        assertThat(problem.getProperties()).containsKey("supportedVersions");
        assertThat(problem.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("createRemoteDownstreamProblem captures remote service failure taxonomy")
    void shouldCaptureRemoteDownstreamProblem() {
        ProblemDetail problem =
                ModernRestApiResponseFactory.createRemoteDownstreamProblem(
                        HttpStatus.BAD_GATEWAY,
                        "payment-service",
                        "Gateway socket timeout",
                        "trace-xyz-987");

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(problem.getTitle()).isEqualTo("Remote Service Dependency Error");
        assertThat(problem.getProperties()).containsEntry("remoteService", "payment-service");
        assertThat(problem.getProperties()).containsEntry("traceId", "trace-xyz-987");
    }
}
