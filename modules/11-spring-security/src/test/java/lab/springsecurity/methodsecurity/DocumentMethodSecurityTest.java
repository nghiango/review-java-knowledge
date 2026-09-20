package lab.springsecurity.methodsecurity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentMethodSecurityTest {

    private DocumentService documentService;
    private UUID docId;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService();
        docId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("Document update persists modified content")
    void updateDocument_validExecution_updatesDocument() {
        Document updated =
                documentService.updateDocument(docId, "alice", "New Financial Report 2026 Content");

        assertThat(updated.content()).isEqualTo("New Financial Report 2026 Content");
        assertThat(updated.owner()).isEqualTo("alice");
    }

    @Test
    @DisplayName("Document lookup returns present optional")
    void findById_existingDocument_returnsDocument() {
        assertThat(documentService.findById(docId)).isPresent();
    }
}
