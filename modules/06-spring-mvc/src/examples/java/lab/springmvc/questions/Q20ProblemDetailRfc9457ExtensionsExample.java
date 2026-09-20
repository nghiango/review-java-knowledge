package lab.springmvc.questions;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class Q20ProblemDetailRfc9457ExtensionsExample {

    record ValidationError(String field, String message) {}

    public static void main(String[] args) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Validation failed for 2 fields");
        problem.setTitle("Invalid Request Content");
        problem.setType(URI.create("https://api.example.com/errors/validation-failed"));
        problem.setProperty(
                "invalidParams",
                List.of(
                        new ValidationError("email", "Must be a well-formed email address"),
                        new ValidationError("age", "Must be greater than or equal to 18")));

        int status = problem.getStatus(); // 400
        boolean hasInvalidParams = problem.getProperties().containsKey("invalidParams"); // true

        System.out.println(
                "Status: " + status + ", has custom extension properties: " + hasInvalidParams);
    }
}
