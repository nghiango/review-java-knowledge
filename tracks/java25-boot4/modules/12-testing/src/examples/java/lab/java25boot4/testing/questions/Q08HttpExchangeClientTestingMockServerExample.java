package lab.java25boot4.testing.questions;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/** Q08: How do you unit test declarative @HttpExchange client interfaces in Spring Boot 4? */
public class Q08HttpExchangeClientTestingMockServerExample {

    @HttpExchange("/orders")
    public interface OrderServiceClient {
        @GetExchange("/{orderId}")
        String getOrderStatus(String orderId);
    }

    public static void main(String[] args) {
        boolean isInterface = OrderServiceClient.class.isInterface();
        boolean hasHttpExchange = OrderServiceClient.class.isAnnotationPresent(HttpExchange.class);

        System.out.println("Is interface: " + isInterface); // Is interface: true
        System.out.println("Has HttpExchange: " + hasHttpExchange); // Has HttpExchange: true
    }
}
