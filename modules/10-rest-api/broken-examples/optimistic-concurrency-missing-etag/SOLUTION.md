# Solution: Optimistic Concurrency & Missing ETag

## Annotated Code

### `DocumentEditorController.java`
```java
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

    // Performance issue: Missing ETag or Last-Modified header on GET response prevents HTTP client caching and conditional GET validation (304 Not Modified).
    // Concurrency issue: Clients have no version validator (ETag) to attach to subsequent mutating requests (If-Match), making race condition detection impossible.
    @GetMapping("/{id}")
    public ResponseEntity<ArticleDocument> getDocument(@PathVariable UUID id) {
        ArticleDocument doc = documentStore.get(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }

    // Concurrency issue: Blind update without evaluating the If-Match header leads to the "Lost Update" anomaly when multiple users edit concurrently.
    // Error Handling issue: Missing 428 Precondition Required when If-Match is omitted, and missing 412 Precondition Failed when If-Match does not match the current resource ETag.
    @PutMapping("/{id}")
    public ResponseEntity<ArticleDocument> updateDocument(
            @PathVariable UUID id,
            @RequestBody UpdateRequest request) {
        ArticleDocument doc = documentStore.get(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        doc.setTitle(request.title());
        doc.setContent(request.content());
        doc.setVersion(doc.getVersion() + 1);
        doc.setUpdatedAt(Instant.now());

        return ResponseEntity.ok(doc);
    }

    public record UpdateRequest(String title, String content) {}
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Missing `ETag` on GET responses | Performance | Major | Omission of the `ETag` (Entity Tag) HTTP header prevents downstream caches, proxies, and web clients from performing conditional requests (`If-None-Match`), wasting server bandwidth and compute. |
| Lost Update anomaly on concurrent PUT | Concurrency | Critical | When two clients fetch version 1 simultaneously and submit updates sequentially, the second write silently overwrites the first write without conflict detection. |
| Missing `If-Match` conditional validation | API Contract | Major | Mutating endpoints (`PUT`, `PATCH`, `DELETE`) on versioned resources should mandate the `If-Match` header to guarantee optimistic concurrency control at the HTTP protocol layer. |
| Missing HTTP `412 Precondition Failed` / `428 Precondition Required` | Error Handling | Major | Omitting precondition enforcement causes APIs to return improper status codes (or false `200 OK`) instead of standard RFC 9110 / RFC 6585 error codes when headers are mismatched or absent. |

## Correct Implementation Reference
- [`SafeDocumentController.java`](../../src/main/java/lab/restapi/concurrency/SafeDocumentController.java)
- [`DocumentService.java`](../../src/main/java/lab/restapi/concurrency/DocumentService.java)
- [`Document.java`](../../src/main/java/lab/restapi/concurrency/Document.java)
