package lab.springmvc.broken.exceptionhandling;

import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    public String findOrder(String id) {
        if ("error".equals(id)) {
            throw new IllegalStateException(
                    "Database query failed: SELECT * FROM orders WHERE id="
                            + id
                            + "; Table 'orders' does not exist in schema 'prod_db'");
        }
        return "Order details for " + id;
    }
}
