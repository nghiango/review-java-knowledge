package lab.springmvc.broken.httpstatus;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping("/{id}")
    public ApiResponse<Product> getProduct(@PathVariable("id") String id) {
        if ("missing".equals(id)) {
            return ApiResponse.error(404, "Product not found");
        }
        return ApiResponse.success(new Product(id, "Laptop", 1200.0));
    }

    @PostMapping
    public ApiResponse<Product> createProduct(@RequestBody Product product) {
        if (product.price() <= 0) {
            return ApiResponse.error(400, "Price must be positive");
        }
        return ApiResponse.success(product);
    }
}
