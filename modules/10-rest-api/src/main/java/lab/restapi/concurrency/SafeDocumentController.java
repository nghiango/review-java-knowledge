package lab.restapi.concurrency;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class SafeDocumentController {

    private final DocumentService documentService;

    public SafeDocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(
            @PathVariable UUID id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {

        return documentService
                .findById(id)
                .map(
                        doc -> {
                            String etag = doc.generateETag();
                            if (ifNoneMatch != null && ifNoneMatch.trim().equals(etag)) {
                                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                                        .eTag(etag)
                                        .<Document>build();
                            }
                            return ResponseEntity.ok().eTag(etag).body(doc);
                        })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody UpdateDocumentRequest request) {

        Document updated = documentService.update(id, request.title(), request.content(), ifMatch);
        return ResponseEntity.ok().eTag(updated.generateETag()).body(updated);
    }

    @ExceptionHandler(PreconditionRequiredException.class)
    public ResponseEntity<String> handlePreconditionRequired(PreconditionRequiredException ex) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(ex.getMessage());
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<String> handlePreconditionFailed(PreconditionFailedException ex) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(ex.getMessage());
    }

    public record UpdateDocumentRequest(@NotBlank String title, @NotBlank String content) {}
}
