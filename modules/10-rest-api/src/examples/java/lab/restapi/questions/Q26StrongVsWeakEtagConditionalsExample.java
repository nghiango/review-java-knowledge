package lab.restapi.questions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("unused")
public final class Q26StrongVsWeakEtagConditionalsExample {
    private Q26StrongVsWeakEtagConditionalsExample() {}

    // Strong ETag vs Weak ETag:
    // Strong ETag ("\"hash\""): Byte-for-byte exact representation match. Required for HTTP Range requests.
    // Weak ETag (W/"\"hash\""): Semantic equivalence (e.g. gzip compressed vs uncompressed, or timestamp-based).
    public static class EtagValidator {
        private String currentStrongEtag = "\"v1-exact-bytes\"";

        public ResponseEntity<String> updateResource(String ifMatchHeader, String newContent) {
            // Concurrency protection: If client's If-Match header does not match current ETag,
            // another user modified the resource concurrently -> return HTTP 412 Precondition Failed!
            if (ifMatchHeader == null || !ifMatchHeader.equals(currentStrongEtag)) {
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .body("Precondition Failed: Resource was modified by another client");
            }

            this.currentStrongEtag = "\"v2-updated-bytes\"";
            return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, currentStrongEtag)
                .body(newContent);
        }

        public String getEtag() { return currentStrongEtag; }
    }

    public static void main(String[] args) {
        EtagValidator validator = new EtagValidator();
        // Client A with stale ETag attempts update:
        ResponseEntity<String> staleAttempt = validator.updateResource("\"v0-stale\"", "new data");
        int status1 = staleAttempt.getStatusCode().value(); // 412 (Precondition Failed, prevents lost update!)

        // Client B with fresh ETag succeeds:
        ResponseEntity<String> validAttempt = validator.updateResource("\"v1-exact-bytes\"", "new data");
        int status2 = validAttempt.getStatusCode().value(); // 200 (OK)
    }
}
