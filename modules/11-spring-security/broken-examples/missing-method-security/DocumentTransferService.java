package lab.springsecurity.broken.methodsecurity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DocumentTransferService {

    private final Map<UUID, DocumentRecord> store = new ConcurrentHashMap<>();

    public DocumentTransferService() {
        UUID doc1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        store.put(doc1, new DocumentRecord(doc1, "alice", "Financial Report 2026", "Confidential quarterly figures"));
    }

    // Missing @PreAuthorize or method security authorization; relies solely on caller or parameter trust
    public DocumentRecord updateDocument(UUID id, String requesterUsername, String newContent) {
        DocumentRecord record = store.get(id);
        if (record == null) {
            throw new IllegalArgumentException("Document not found");
        }

        // Trusting unverified client parameter instead of evaluating against SecurityContext principal
        DocumentRecord updated = new DocumentRecord(record.id(), record.owner(), record.title(), newContent);
        store.put(id, updated);
        return updated;
    }

    public DocumentRecord getDocument(UUID id) {
        return store.get(id);
    }

    public record DocumentRecord(UUID id, String owner, String title, String content) {}
}
