package lab.java25boot4.restapi.questions;

import org.springframework.http.MediaType;

/**
 * Q08: How does vendor media-type content negotiation versioning work in REST, and how is it
 * implemented cleanly in Spring Boot 4?
 */
public class Q08ContentNegotiationVersioningExample {

    public static final MediaType ORDER_V1_JSON =
            MediaType.parseMediaType("application/vnd.company.order.v1+json");
    public static final MediaType ORDER_V2_JSON =
            MediaType.parseMediaType("application/vnd.company.order.v2+json");

    public static String routeRepresentation(MediaType acceptHeader) {
        if (acceptHeader.isCompatibleWith(ORDER_V1_JSON)) {
            return "OrderV1Representation";
        } else if (acceptHeader.isCompatibleWith(ORDER_V2_JSON)) {
            return "OrderV2Representation";
        }
        return "DefaultV2Representation";
    }

    public static void main(String[] args) {
        MediaType clientAcceptV1 =
                MediaType.parseMediaType("application/vnd.company.order.v1+json");
        MediaType clientAcceptV2 =
                MediaType.parseMediaType("application/vnd.company.order.v2+json");

        System.out.println(
                "V1 Route: " + routeRepresentation(clientAcceptV1)); // "OrderV1Representation"
        System.out.println(
                "V2 Route: " + routeRepresentation(clientAcceptV2)); // "OrderV2Representation"
        System.out.println("Subtype: " + ORDER_V2_JSON.getSubtype()); // "vnd.company.order.v2+json"
    }
}
