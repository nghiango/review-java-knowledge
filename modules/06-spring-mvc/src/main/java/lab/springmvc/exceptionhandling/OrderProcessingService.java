package lab.springmvc.exceptionhandling;

import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    public String findOrder(String id) {
        if ("missing".equals(id)) {
            throw new OrderNotFoundException(id);
        }
        if ("error".equals(id)) {
            throw new IllegalStateException("Internal database connection failure");
        }
        return "Order details for " + id;
    }
}
