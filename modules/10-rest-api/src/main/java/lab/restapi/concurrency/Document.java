package lab.restapi.concurrency;

import java.time.Instant;
import java.util.UUID;

public record Document(UUID id, String title, String content, long version, Instant updatedAt) {

    public String generateETag() {
        return "\"" + version + "-" + id + "\"";
    }
}
