package lab.java25boot4.testing.questions;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Q04: How do you verify RFC 9457 ProblemDetail payloads in modern integration tests? */
public class Q04ProblemDetailTestingAssertionsExample {

    public static void main(String[] args) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Order not found: ord-99");
        problem.setTitle("Order Missing");
        problem.setType(URI.create("https://api.example.com/errors/not-found"));
        problem.setProperty("orderId", "ord-99");

        boolean matchesStatus = problem.getStatus() == 404;
        boolean matchesDetail = problem.getDetail().contains("ord-99");
        boolean hasProperty = "ord-99".equals(problem.getProperties().get("orderId"));

        System.out.println("Matches status: " + matchesStatus); // Matches status: true
        System.out.println("Matches detail: " + matchesDetail); // Matches detail: true
        System.out.println("Has property: " + hasProperty); // Has property: true
    }
}
