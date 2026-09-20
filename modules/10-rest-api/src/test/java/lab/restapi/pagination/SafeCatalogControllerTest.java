package lab.restapi.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SafeCatalogControllerTest {

    private CatalogItemRepository repository;
    private SafeCatalogController controller;

    @BeforeEach
    void setUp() {
        repository = new CatalogItemRepository();
        controller = new SafeCatalogController(repository);
    }

    @Test
    @DisplayName("getCatalog returns bounded page of items and metadata")
    void getCatalog_pageZero_returnsBoundedPage() {
        ResponseEntity<PagedResponse<CatalogItem>> response = controller.getCatalog(null, 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(10);
        assertThat(response.getBody().page().totalElements()).isEqualTo(150);
        assertThat(response.getBody().page().totalPages()).isEqualTo(15);
        assertThat(response.getBody().page().hasNext()).isTrue();
        assertThat(response.getBody().page().hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("getCatalog enforces max size cap to prevent resource exhaustion")
    void getCatalog_requestedSizeOverMax_capsToMaxSize() {
        // Request size 500 should be capped to 100
        ResponseEntity<PagedResponse<CatalogItem>> response = controller.getCatalog(null, 0, 500);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(100);
        assertThat(response.getBody().page().size()).isEqualTo(100);
    }
}
