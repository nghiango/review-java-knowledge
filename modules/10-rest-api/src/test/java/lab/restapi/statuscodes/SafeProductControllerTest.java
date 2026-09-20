package lab.restapi.statuscodes;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SafeProductControllerTest {

    private ProductService productService;
    private SafeProductController controller;

    @BeforeEach
    void setUp() {
        productService = new ProductService();
        controller = new SafeProductController(productService);
    }

    @Test
    @DisplayName("POST /products returns 201 Created and Location header")
    void createProduct_validPayload_returns201() {
        var req = new CreateProductRequest("SKU-100", "Widget", new BigDecimal("19.99"));
        ResponseEntity<Product> response = controller.createProduct(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody().sku()).isEqualTo("SKU-100");
    }

    @Test
    @DisplayName("GET /products/{id} returns 200 OK when found, 404 when absent")
    void getProduct_existingAndMissing_returnsCorrectCodes() {
        var req = new CreateProductRequest("SKU-200", "Gadget", new BigDecimal("29.99"));
        Product created = productService.create(req);

        ResponseEntity<Product> found = controller.getProduct(created.id());
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Product> notFound = controller.getProduct(UUID.randomUUID());
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("DELETE /products/{id} returns 204 No Content when deleted, 404 when absent")
    void deleteProduct_existingAndMissing_returnsCorrectCodes() {
        var req = new CreateProductRequest("SKU-300", "Book", new BigDecimal("9.99"));
        Product created = productService.create(req);

        ResponseEntity<Void> deleted = controller.deleteProduct(created.id());
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Void> missing = controller.deleteProduct(created.id());
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
