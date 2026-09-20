package lab.springmvc.questions;

import java.util.List;
import org.springframework.http.MediaType;

public class Q11ContentNegotiationExample {

    public static void main(String[] args) {
        // Content Negotiation matches client Accept header against producer MediaTypes
        List<MediaType> clientAccepted =
                List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);
        List<MediaType> serverProducible = List.of(MediaType.APPLICATION_JSON);

        boolean supportsJson =
                clientAccepted.contains(MediaType.APPLICATION_JSON)
                        && serverProducible.contains(MediaType.APPLICATION_JSON); // true
        boolean serverProducesXml = serverProducible.contains(MediaType.APPLICATION_XML); // false

        System.out.println(
                "JSON matched: " + supportsJson + ", XML produced: " + serverProducesXml);
    }
}
