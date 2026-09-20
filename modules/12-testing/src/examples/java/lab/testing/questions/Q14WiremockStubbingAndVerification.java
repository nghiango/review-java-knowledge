package lab.testing.questions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Q14: WireMock stubbing and verification.
 *
 * <p>A stub is a *matching rule*: method, URL pattern, headers, body. A request that matches no
 * stub gets 404 with "Request was not matched" — which is why a near miss (a renamed path, a
 * case-different id, a missing {@code Accept} header) fails loudly instead of returning an empty
 * body. Verification is the mirror image: it counts the recorded requests that match a request
 * pattern, so {@code verify(getRequestedFor(urlEqualTo(...)).withHeader("Accept", equalTo(...)))}
 * is what proves the client sent the request the contract describes.
 */
public class Q14WiremockStubbingAndVerification {

    /** A recorded request as WireMock sees it. */
    record Request(String method, String path, Map<String, String> headers) {

        /** Header names are case-insensitive; WireMock normalises them before matching. */
        Optional<String> header(String name) {
            return Optional.ofNullable(headers.get(name.toLowerCase(Locale.ROOT)));
        }
    }

    /** A stub: what to answer when a request matches every part of the rule. */
    record Stub(
            String method, String path, Optional<String> requiredAccept, int status, String body) {}

    public static void main(String[] args) {
        // The stub the module's OrderFulfilmentWireMockIT registers for the inventory service.
        Stub inventoryStub =
                new Stub(
                        "GET",
                        "/inventory/A-1",
                        Optional.of("application/json"),
                        200,
                        "{\"sku\":\"A-1\",\"available\":3}");
        List<Stub> stubs = List.of(inventoryStub);

        List<Request> recorded = new ArrayList<>();
        Request fromClient =
                new Request("GET", "/inventory/A-1", Map.of("accept", "application/json"));
        Request nearMiss =
                new Request("GET", "/inventory/a-1", Map.of("accept", "application/json"));
        Request wrongAccept = new Request("GET", "/inventory/A-1", Map.of("accept", "text/plain"));

        recorded.add(fromClient);

        int matchedStatus = match(stubs, fromClient).map(Stub::status).orElse(404); // 200
        int nearMissStatus = match(stubs, nearMiss).map(Stub::status).orElse(404); // 404
        int wrongAcceptStatus = match(stubs, wrongAccept).map(Stub::status).orElse(404); // 404

        int recordedCount = recorded.size(); // 1
        long verifiedCount = countMatching(recorded, fromClient); // 1
        long wrongPathCount = countMatching(recorded, nearMiss); // 0
        boolean verificationPasses = verifiedCount == 1; // true

        System.out.println("Matched: " + matchedStatus); // Matched: 200
        System.out.println("Near miss: " + nearMissStatus); // Near miss: 404
        System.out.println("Accept mismatch: " + wrongAcceptStatus); // Accept mismatch: 404
        System.out.println("Recorded: " + recordedCount); // Recorded: 1
        System.out.println("Verified: " + verificationPasses); // Verified: true
        System.out.println("Wrong path: " + wrongPathCount); // Wrong path: 0
    }

    /** Finds the first stub whose rule the request matches; an unmatched request is a 404. */
    private static Optional<Stub> match(List<Stub> stubs, Request request) {
        return stubs.stream()
                .filter(stub -> stub.method().equals(request.method()))
                .filter(stub -> stub.path().equals(request.path()))
                .filter(stub -> accepts(stub, request))
                .findFirst();
    }

    /**
     * A stub with no {@code Accept} requirement matches anything; otherwise the header must match.
     */
    private static boolean accepts(Stub stub, Request request) {
        Optional<String> required = stub.requiredAccept();
        if (required.isEmpty()) {
            return true;
        }
        return request.header("accept").filter(required.orElseThrow()::equals).isPresent();
    }

    /** Counts the recorded requests that match a verification pattern. */
    private static long countMatching(List<Request> recorded, Request pattern) {
        return recorded.stream()
                .filter(request -> request.method().equals(pattern.method()))
                .filter(request -> request.path().equals(pattern.path()))
                .count();
    }
}
