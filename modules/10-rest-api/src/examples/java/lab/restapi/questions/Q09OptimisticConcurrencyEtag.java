package lab.restapi.questions;

import org.springframework.http.HttpStatus;

public class Q09OptimisticConcurrencyEtag {

    public static void main(String[] args) {
        String currentServerEtag = "\"version-3\"";

        // Client 1 sends matching ETag in If-Match
        String clientIfMatchValid = "\"version-3\"";
        HttpStatus statusValid =
                validateIfMatch(currentServerEtag, clientIfMatchValid); // HttpStatus.OK
        boolean isUpdateAllowed = (statusValid == HttpStatus.OK); // true

        // Client 2 sends outdated ETag in If-Match
        String clientIfMatchStale = "\"version-2\"";
        HttpStatus statusStale =
                validateIfMatch(
                        currentServerEtag, clientIfMatchStale); // HttpStatus.PRECONDITION_FAILED
        boolean isConflictDetected = (statusStale == HttpStatus.PRECONDITION_FAILED); // true

        // Client 3 omits If-Match
        HttpStatus statusMissing =
                validateIfMatch(currentServerEtag, null); // HttpStatus.PRECONDITION_REQUIRED
        boolean isPreconditionRequired =
                (statusMissing == HttpStatus.PRECONDITION_REQUIRED); // true

        System.out.println(
                "Valid update allowed: " + isUpdateAllowed); // Valid update allowed: true
        System.out.println(
                "Stale update rejected: " + isConflictDetected); // Stale update rejected: true
        System.out.println(
                "Missing header rejected: "
                        + isPreconditionRequired); // Missing header rejected: true
    }

    public static HttpStatus validateIfMatch(String serverEtag, String ifMatchHeader) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            return HttpStatus.PRECONDITION_REQUIRED; // 428
        }
        if (!"*".equals(ifMatchHeader) && !serverEtag.equals(ifMatchHeader)) {
            return HttpStatus.PRECONDITION_FAILED; // 412
        }
        return HttpStatus.OK;
    }
}
