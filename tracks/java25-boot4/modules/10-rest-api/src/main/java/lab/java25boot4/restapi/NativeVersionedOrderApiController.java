package lab.java25boot4.restapi;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Production REST controller demonstrating declarative API versioning, RFC 8594 lifecycle headers,
 * and RFC 9457 error contracts.
 */
@RestController
@RequestMapping("/api/orders")
public class NativeVersionedOrderApiController {

    public static final String VENDOR_MEDIA_TYPE_V1 = "application/vnd.orders.v1+json";
    public static final String VENDOR_MEDIA_TYPE_V2 = "application/vnd.orders.v2+json";
    public static final String HEADER_API_VERSION = "X-API-Version";

    private static final List<String> SUPPORTED_VERSIONS = List.of("1", "2");
    private static final ZonedDateTime V1_DEPRECATION =
            ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final ZonedDateTime V1_SUNSET =
            ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
    private static final String MIGRATION_GUIDE_URL =
            "https://docs.example.com/api/orders/migration-v2";

    /** V1 endpoint: Deprecated with RFC 8594 Sunset and Deprecation headers. */
    @GetMapping(
            value = "/{id}",
            headers = HEADER_API_VERSION + "=1",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderV1Representation> getOrderV1ByHeader(@PathVariable String id) {
        return buildV1Response(id);
    }

    @GetMapping(value = "/{id}", produces = VENDOR_MEDIA_TYPE_V1)
    public ResponseEntity<OrderV1Representation> getOrderV1ByMediaType(@PathVariable String id) {
        return buildV1Response(id);
    }

    /** V2 endpoint: Current canonical representation. */
    @GetMapping(
            value = "/{id}",
            headers = HEADER_API_VERSION + "=2",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderV2Representation> getOrderV2ByHeader(@PathVariable String id) {
        return buildV2Response(id);
    }

    @GetMapping(value = "/{id}", produces = VENDOR_MEDIA_TYPE_V2)
    public ResponseEntity<OrderV2Representation> getOrderV2ByMediaType(@PathVariable String id) {
        return buildV2Response(id);
    }

    /** Fallback dispatcher when version header is present but invalid or unsupported. */
    @GetMapping(value = "/{id}", headers = HEADER_API_VERSION)
    public ResponseEntity<ProblemDetail> handleInvalidVersionHeader(
            @PathVariable String id, @RequestHeader(HEADER_API_VERSION) String requestedVersion) {
        if (!SUPPORTED_VERSIONS.contains(requestedVersion)) {
            ProblemDetail problem =
                    ModernRestApiResponseFactory.createUnsupportedVersionProblem(
                            requestedVersion, SUPPORTED_VERSIONS);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
        }
        // Default to V2 if version "2" matches or routed here
        return ResponseEntity.ok().build();
    }

    /** Default unversioned GET defaults to modern V2. */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderV2Representation> getOrderDefault(@PathVariable String id) {
        return buildV2Response(id);
    }

    /** V2 Create Order endpoint returning 201 Created with Location. */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderV2Representation> createOrderV2(
            @RequestBody CreateOrderCommand command) {
        String generatedId = "ORD-" + System.currentTimeMillis();
        OrderV2Representation representation =
                new OrderV2Representation(
                        generatedId,
                        command.customerId(),
                        command.amountCents(),
                        "CONFIRMED",
                        command.currency());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/orders/" + generatedId)
                .body(representation);
    }

    private ResponseEntity<OrderV1Representation> buildV1Response(String orderId) {
        HttpHeaders headers =
                ModernRestApiResponseFactory.createLifecycleHeaders(
                        V1_DEPRECATION, V1_SUNSET, MIGRATION_GUIDE_URL);
        OrderV1Representation representation =
                new OrderV1Representation(orderId, "CUST-42", 99.50, "FULFILLED");
        return ResponseEntity.ok().headers(headers).body(representation);
    }

    private ResponseEntity<OrderV2Representation> buildV2Response(String orderId) {
        OrderV2Representation representation =
                new OrderV2Representation(orderId, "CUST-42", 9950L, "FULFILLED", "USD");
        return ResponseEntity.ok(representation);
    }

    public record OrderV1Representation(
            String orderId, String customerId, double totalAmount, String status) {}

    public record OrderV2Representation(
            String orderId, String customerId, long amountCents, String status, String currency) {}

    public record CreateOrderCommand(String customerId, long amountCents, String currency) {}
}
