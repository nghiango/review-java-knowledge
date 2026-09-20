package lab.springsecurity.broken.methodsecurity;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class InsecureDocumentController {

    private final DocumentTransferService documentService;

    public InsecureDocumentController(DocumentTransferService documentService) {
        this.documentService = documentService;
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentTransferService.DocumentRecord> updateDocument(
            @PathVariable UUID id,
            @RequestParam String username,
            @RequestBody UpdateRequest request) {

        // Takes username directly from query param; any user can supply username="alice" and modify Alice's document
        DocumentTransferService.DocumentRecord updated =
                documentService.updateDocument(id, username, request.content());
        return ResponseEntity.ok(updated);
    }

    public record UpdateRequest(String content) {}
}
