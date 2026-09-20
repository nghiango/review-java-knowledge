package lab.restapi.questions;

public class Q11ApiVersioningStrategies {

    public static void main(String[] args) {
        // Strategy 1: URI Path Versioning (most common, explicit in access logs and caches)
        String pathVersionedUri = "/api/v2/orders/123";
        boolean isPathV2 = pathVersionedUri.contains("/v2/"); // true

        // Strategy 2: Query Parameter Versioning
        String queryVersionedUri = "/api/orders/123?version=2";
        boolean isQueryV2 = queryVersionedUri.contains("version=2"); // true

        // Strategy 3: Custom Header Versioning
        String customHeaderName = "X-API-Version";
        String customHeaderValue = "2";
        boolean isHeaderV2 = "2".equals(customHeaderValue); // true

        // Strategy 4: Content Negotiation (Accept header / vendor media type)
        String acceptHeader = "application/vnd.company.app-v2+json";
        boolean isMediaV2 = acceptHeader.contains("app-v2"); // true

        System.out.println("Path versioning matched: " + isPathV2); // Path versioning matched: true
        System.out.println(
                "Query versioning matched: "
                        + isQueryV2
                        + " on "
                        + queryVersionedUri); // Query versioning matched: true on
        // /api/orders/123?version=2
        System.out.println(
                "Header "
                        + customHeaderName
                        + " versioning matched: "
                        + isHeaderV2); // Header X-API-Version versioning matched: true
        System.out.println(
                "Accept header "
                        + acceptHeader
                        + " matched: "
                        + isMediaV2); // Accept header application/vnd.company.app-v2+json matched:
        // true
    }
}
