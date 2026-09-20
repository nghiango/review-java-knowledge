package lab.restapi.broken.etagconcurrency;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentEditorController {

    private final Map<UUID, ArticleDocument> documentStore = new ConcurrentHashMap<>();

    public DocumentEditorController() {
        UUID docId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        documentStore.put(docId, new ArticleDocument(docId, "Draft Architecture", "Initial draft...", 1L, Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArticleDocument> getDocument(@PathVariable UUID id) {
        ArticleDocument doc = documentStore.get(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        // Returns resource without ETag header or Last-Modified header
        return ResponseEntity.ok(doc);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleDocument> updateDocument(
            @PathVariable UUID id,
            @RequestBody UpdateRequest request) {
        ArticleDocument doc = documentStore.get(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        // Blind overwrite without checking If-Match header; causes Lost Update anomaly
        doc.setTitle(request.title());
        doc.setContent(request.content());
        doc.setVersion(doc.getVersion() + 1);
        doc.setUpdatedAt(Instant.now());

        return ResponseEntity.ok(doc);
    }

    public record UpdateRequest(String title, String content) {}
}
