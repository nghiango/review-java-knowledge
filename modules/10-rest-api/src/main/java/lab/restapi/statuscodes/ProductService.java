package lab.restapi.statuscodes;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final Map<UUID, Product> products = new ConcurrentHashMap<>();

    public Product create(CreateProductRequest request) {
        boolean skuExists =
                products.values().stream().anyMatch(p -> p.sku().equalsIgnoreCase(request.sku()));
        if (skuExists) {
            throw new IllegalArgumentException("Product with SKU already exists: " + request.sku());
        }
        UUID id = UUID.randomUUID();
        Product product = new Product(id, request.sku(), request.name(), request.price());
        products.put(id, product);
        return product;
    }

    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }

    public Optional<Product> update(UUID id, UpdateProductRequest request) {
        return Optional.ofNullable(
                products.computeIfPresent(
                        id,
                        (key, existing) ->
                                new Product(
                                        existing.id(),
                                        existing.sku(),
                                        request.name(),
                                        request.price())));
    }

    public boolean delete(UUID id) {
        return products.remove(id) != null;
    }
}
