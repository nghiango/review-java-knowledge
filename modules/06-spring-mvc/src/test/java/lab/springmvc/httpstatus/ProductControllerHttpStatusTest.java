package lab.springmvc.httpstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

class ProductControllerHttpStatusTest {

    private final ProductService service = new ProductService();
    private final ProductController controller = new ProductController(service);

    @Test
    @DisplayName("Create product should return HTTP 201 Created with Location header")
    void createProduct_returns201WithLocation() {
        Product product = new Product("p-101", "Mechanical Keyboard", 150.0);
        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder.newInstance().scheme("https").host("api.example.com");

        ResponseEntity<Product> response = controller.createProduct(product, uriBuilder);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().toString())
                .isEqualTo("https://api.example.com/api/products/p-101");
        assertThat(response.getBody()).isEqualTo(product);
    }

    @Test
    @DisplayName("Get product should return HTTP 200 OK")
    void getProduct_returns200() {
        Product product = new Product("p-102", "Mouse", 50.0);
        service.save(product);

        ResponseEntity<Product> response = controller.getProduct("p-102");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(product);
    }

    @Test
    @DisplayName("Delete product should return HTTP 204 No Content")
    void deleteProduct_returns204() {
        Product product = new Product("p-103", "Monitor", 300.0);
        service.save(product);

        ResponseEntity<Void> response = controller.deleteProduct("p-103");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThatThrownBy(() -> service.findById("p-103"))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
