package lab.java25boot4.restapi.questions;

import java.util.Map;

/**
 * Q02: What are the primary REST API versioning strategies supported natively in Spring Framework
 * 7, and how do their trade-offs compare?
 */
public class Q02NativeApiVersioningStrategiesExample {

    public enum VersioningStrategy {
        URI_PATH("/api/v1/orders", "Explicit and cache-friendly; duplicates URL hierarchy"),
        HEADER(
                "X-API-Version: 2",
                "Clean URIs; requires custom header inspection and cache variation"),
        QUERY_PARAM(
                "/api/orders?version=2",
                "Convenient for exploratory testing; mixes resource identity with parameters"),
        MEDIA_TYPE(
                "Accept: application/vnd.company.v2+json",
                "Pure REST HATEOAS / content negotiation; complex client setup");

        private final String example;
        private final String tradeOff;

        VersioningStrategy(String example, String tradeOff) {
            this.example = example;
            this.tradeOff = tradeOff;
        }

        public String getExample() {
            return example;
        }

        public String getTradeOff() {
            return tradeOff;
        }
    }

    public static void main(String[] args) {
        var strategies =
                Map.of(
                        "PATH", VersioningStrategy.URI_PATH.getExample(),
                        "HEADER", VersioningStrategy.HEADER.getExample(),
                        "MEDIA_TYPE", VersioningStrategy.MEDIA_TYPE.getExample());

        System.out.println("Path example: " + strategies.get("PATH")); // "/api/v1/orders"
        System.out.println("Header example: " + strategies.get("HEADER")); // "X-API-Version: 2"
        System.out.println(
                "Media type example: "
                        + strategies.get(
                                "MEDIA_TYPE")); // "Accept: application/vnd.company.v2+json"
    }
}
