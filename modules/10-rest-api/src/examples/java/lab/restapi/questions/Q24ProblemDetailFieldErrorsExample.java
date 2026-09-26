package lab.restapi.questions;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@SuppressWarnings("unused")
public final class Q24ProblemDetailFieldErrorsExample {
    private Q24ProblemDetailFieldErrorsExample() {}

    public record InvalidParameter(String name, String reason) {}

    // RFC 9457 Problem Details extension properties:
    // Allows attaching structured domain metadata (e.g. invalid-params list, correlation ID, error
    // code)
    // to standard HTTP error responses without breaking RFC compliance.
    public static ProblemDetail buildValidationProblem(
            List<InvalidParameter> fieldErrors, String correlationId) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Request payload validation failed");
        problem.setType(URI.create("https://api.example.com/errors/validation-failed"));
        problem.setTitle("Invalid Request Parameters");
        problem.setProperty("correlationId", correlationId);
        problem.setProperty("invalidParams", fieldErrors);
        return problem;
    }

    public static void main(String[] args) {
        List<InvalidParameter> errors =
                List.of(
                        new InvalidParameter("email", "Must be a well-formed email address"),
                        new InvalidParameter("amount", "Must be greater than 0"));

        ProblemDetail problem = buildValidationProblem(errors, "corr-xyz-789");
        Map<String, Object> properties = problem.getProperties();
        boolean hasParams = properties != null && properties.containsKey("invalidParams"); // true
    }
}
