package lab.java25boot4.springsecurity.questions;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Q07: How has Spring Security modernized method security annotations and SpEL evaluations in
 * modern Spring applications?
 */
public class Q07MethodSecurityPreAuthorizeSpelExample {

    public interface OrderService {
        @PreAuthorize("hasRole('ADMIN') or #customerId == authentication.name")
        String cancelOrder(String orderId, String customerId);
    }

    public static void main(String[] args) {
        boolean hasPreAuthorize = false;
        try {
            var method = OrderService.class.getMethod("cancelOrder", String.class, String.class);
            hasPreAuthorize = method.isAnnotationPresent(PreAuthorize.class);
        } catch (NoSuchMethodException ignored) {
        }

        System.out.println("Has @PreAuthorize: " + hasPreAuthorize); // true
        System.out.println("Target method: cancelOrder"); // "cancelOrder"
    }
}
