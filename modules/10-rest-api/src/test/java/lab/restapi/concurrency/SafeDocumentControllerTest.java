package lab.restapi.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SafeDocumentControllerTest {

    private DocumentService documentService;
    private SafeDocumentController controller;
    private UUID docId;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService();
        controller = new SafeDocumentController(documentService);
        docId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @DisplayName("GET /documents/{id} returns 200 OK with ETag header")
    void getDocument_noCacheHeader_returns200WithETag() {
        ResponseEntity<Document> response = controller.getDocument(docId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"1-" + docId + "\"");
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("GET /documents/{id} with matching If-None-Match returns 304 Not Modified")
    void getDocument_matchingIfNoneMatch_returns304() {
        String etag = "\"1-" + docId + "\"";
        ResponseEntity<Document> response = controller.getDocument(docId, etag);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("PUT /documents/{id} without If-Match throws PreconditionRequiredException")
    void updateDocument_missingIfMatch_throwsPreconditionRequired() {
        var req = new SafeDocumentController.UpdateDocumentRequest("New Title", "Updated content");

        assertThatThrownBy(() -> controller.updateDocument(docId, null, req))
                .isInstanceOf(PreconditionRequiredException.class);
    }

    @Test
    @DisplayName("PUT /documents/{id} with stale If-Match throws PreconditionFailedException")
    void updateDocument_staleIfMatch_throwsPreconditionFailed() {
        var req = new SafeDocumentController.UpdateDocumentRequest("New Title", "Updated content");

        assertThatThrownBy(() -> controller.updateDocument(docId, "\"99-" + docId + "\"", req))
                .isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    @DisplayName("PUT /documents/{id} with valid If-Match succeeds and increments version ETag")
    void updateDocument_validIfMatch_updatesAndReturnsNewETag() {
        String currentETag = "\"1-" + docId + "\"";
        var req = new SafeDocumentController.UpdateDocumentRequest("New Title", "Updated content");

        ResponseEntity<Document> response = controller.updateDocument(docId, currentETag, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"2-" + docId + "\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("New Title");
        assertThat(response.getBody().version()).isEqualTo(2L);
    }
}
