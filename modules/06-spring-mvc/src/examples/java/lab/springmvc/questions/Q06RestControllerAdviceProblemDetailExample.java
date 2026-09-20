package lab.springmvc.questions;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class Q06RestControllerAdviceProblemDetailExample {

    public static void main(String[] args) {
        // RFC 9457 ProblemDetail standardizes error payloads
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND, "User with ID usr-404 was not found");
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://api.example.com/errors/not-found"));
        problem.setProperty("userId", "usr-404");

        int status = problem.getStatus(); // 404
        String title = problem.getTitle(); // "Resource Not Found"
        Object userIdProp = problem.getProperties().get("userId"); // "usr-404"

        System.out.println("Status: " + status + ", title: " + title + ", userId: " + userIdProp);
    }
}
