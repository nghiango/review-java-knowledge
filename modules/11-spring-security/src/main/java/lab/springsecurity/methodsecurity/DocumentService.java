package lab.springsecurity.methodsecurity;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final Map<UUID, Document> store = new ConcurrentHashMap<>();

    public DocumentService() {
        UUID doc1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        store.put(
                doc1,
                new Document(
                        doc1, "alice", "Financial Report 2026", "Confidential quarterly figures"));
    }

    @PreAuthorize("hasRole('ADMIN') or #owner == authentication.name")
    public Document updateDocument(UUID id, String owner, String newContent) {
        Document current = store.get(id);
        if (current == null) {
            throw new IllegalArgumentException("Document not found: " + id);
        }

        Document updated = new Document(current.id(), current.owner(), current.title(), newContent);
        store.put(id, updated);
        return updated;
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public Optional<Document> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteDocument(UUID id) {
        store.remove(id);
    }
}
