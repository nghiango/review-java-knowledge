package lab.restapi.questions;

import java.util.List;
import org.springframework.http.MediaType;

public class Q19ContentNegotiationInternals {

    public static void main(String[] args) {
        // Supported server media types in order of preference
        List<MediaType> serverProducible =
                List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML);

        // Client requested media types from Accept header
        List<MediaType> clientAccepted =
                MediaType.parseMediaTypes(
                        "text/html, application/xhtml+xml, application/json;q=0.9, */*;q=0.8");

        // Negotiate compatible media type
        MediaType selected =
                serverProducible.stream()
                        .filter(
                                serverType ->
                                        clientAccepted.stream()
                                                .anyMatch(
                                                        clientType ->
                                                                clientType.isCompatibleWith(
                                                                        serverType)))
                        .findFirst()
                        .orElse(MediaType.APPLICATION_OCTET_STREAM);

        boolean matchedJson = MediaType.APPLICATION_JSON.equals(selected); // true
        String negotiatedContentType = selected.toString(); // "application/json"

        System.out.println(
                "Negotiated Media Type: "
                        + negotiatedContentType); // Negotiated Media Type: application/json
        System.out.println("Matched JSON: " + matchedJson); // Matched JSON: true
    }
}
