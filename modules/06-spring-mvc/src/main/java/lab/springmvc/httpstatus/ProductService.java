package lab.springmvc.httpstatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    public Product save(Product product) {
        store.put(product.id(), product);
        return product;
    }

    public Product findById(String id) {
        Product product = store.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public void delete(String id) {
        if (!store.containsKey(id)) {
            throw new ProductNotFoundException(id);
        }
        store.remove(id);
    }
}
