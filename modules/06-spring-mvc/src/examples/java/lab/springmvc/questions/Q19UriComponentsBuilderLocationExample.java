package lab.springmvc.questions;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

public class Q19UriComponentsBuilderLocationExample {

    public static void main(String[] args) {
        // UriComponentsBuilder builds encoded RFC-compliant URLs
        URI location =
                UriComponentsBuilder.fromUriString("https://api.example.com/v1")
                        .path("/orders/{id}/items")
                        .queryParam("filter", "active & pending")
                        .buildAndExpand("ord-9021")
                        .encode()
                        .toUri();

        String uriString = location.toString();
        boolean containsId = uriString.contains("ord-9021"); // true
        boolean isEncoded = uriString.contains("%26"); // true (space & space encoded)

        System.out.println(
                "Constructed URI: " + uriString + ", valid: " + (containsId && isEncoded));
    }
}
