package lab.restapi.questions;

import java.util.HashMap;
import java.util.Map;

public class Q13HateoasHypermedia {

    public record Link(String href, String rel) {}

    public static void main(String[] args) {
        // HATEOAS representation: Payload contains data + actionable links
        Map<String, Object> orderResource = new HashMap<>();
        orderResource.put("orderId", "12345");
        orderResource.put("status", "SHIPPED");

        Map<String, Link> links =
                Map.of(
                        "self", new Link("/api/orders/12345", "self"),
                        "customer", new Link("/api/customers/cust-9", "customer"),
                        "tracking", new Link("/api/shipments/track/track-99", "tracking"));
        orderResource.put("_links", links);

        boolean hasSelfLink = links.containsKey("self"); // true
        boolean hasTrackingLink = links.containsKey("tracking"); // true
        String selfHref = links.get("self").href(); // "/api/orders/12345"

        System.out.println(
                "Resource payload keys: "
                        + orderResource
                                .keySet()); // Resource payload keys: [status, _links, orderId]
        System.out.println("Contains self link: " + hasSelfLink); // Contains self link: true
        System.out.println("Self href: " + selfHref); // Self href: /api/orders/12345
        System.out.println(
                "Contains tracking link: " + hasTrackingLink); // Contains tracking link: true
    }
}
