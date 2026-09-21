package lab.java25boot4.restapi.questions;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Q01: How does Spring Boot 4 / Spring Framework 7 declarative @HttpExchange simplify REST client
 * definitions compared to RestClient?
 */
public class Q01HttpExchangeBasicsExample {

    @HttpExchange("/api/v1/customers")
    public interface CustomerClient {
        @GetExchange("/{id}")
        CustomerDto getCustomer(@PathVariable String id);
    }

    public record CustomerDto(String id, String name, String tier) {}

    public static void main(String[] args) {
        CustomerDto sample = new CustomerDto("CUST-1", "Alice", "PLATINUM");

        boolean isHttpExchangeInterface = CustomerClient.class.isInterface();
        boolean hasAnnotation = CustomerClient.class.isAnnotationPresent(HttpExchange.class);

        System.out.println("Is Interface: " + isHttpExchangeInterface); // true
        System.out.println("Has @HttpExchange: " + hasAnnotation); // true
        System.out.println("Sample Customer: " + sample.tier()); // "PLATINUM"
    }
}
