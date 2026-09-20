package lab.restapi.concurrency;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final Map<UUID, Document> documents = new ConcurrentHashMap<>();

    public DocumentService() {
        UUID docId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        documents.put(
                docId,
                new Document(docId, "Draft Architecture", "Initial draft...", 1L, Instant.now()));
    }

    public Optional<Document> findById(UUID id) {
        return Optional.ofNullable(documents.get(id));
    }

    public Document update(UUID id, String title, String content, String ifMatchHeader) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new PreconditionRequiredException(
                    "If-Match header is required for updating document " + id);
        }

        Document current = documents.get(id);
        if (current == null) {
            throw new IllegalArgumentException("Document not found: " + id);
        }

        String currentETag = current.generateETag();
        String normalizedIfMatch = ifMatchHeader.trim();

        if (!"*".equals(normalizedIfMatch) && !currentETag.equals(normalizedIfMatch)) {
            throw new PreconditionFailedException(
                    "ETag mismatch. Provided: " + normalizedIfMatch + ", Current: " + currentETag);
        }

        Document updated =
                new Document(current.id(), title, content, current.version() + 1, Instant.now());

        documents.put(id, updated);
        return updated;
    }
}
