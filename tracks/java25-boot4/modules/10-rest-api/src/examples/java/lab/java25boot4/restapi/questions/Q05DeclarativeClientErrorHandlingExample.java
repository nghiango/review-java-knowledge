package lab.java25boot4.restapi.questions;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Q05: How should declarative HTTP interface clients capture and propagate remote RFC 9457 Problem
 * Details without swallowing error diagnostics?
 */
public class Q05DeclarativeClientErrorHandlingExample {

    public static class RemoteApiException extends RuntimeException {
        private final ProblemDetail problemDetail;

        public RemoteApiException(ProblemDetail problemDetail) {
            super("Remote API error: " + problemDetail.getTitle());
            this.problemDetail = problemDetail;
        }

        public ProblemDetail getProblemDetail() {
            return problemDetail;
        }
    }

    public static void main(String[] args) {
        ProblemDetail remoteProblem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.PAYMENT_REQUIRED, "Account balance insufficient for operation");
        remoteProblem.setTitle("Payment Required");
        remoteProblem.setType(URI.create("https://api.example.com/errors/insufficient-funds"));
        remoteProblem.setProperty("balanceCents", 500L);
        remoteProblem.setProperty("requiredCents", 1200L);

        RemoteApiException exception = new RemoteApiException(remoteProblem);

        System.out.println("Status: " + exception.getProblemDetail().getStatus()); // 402
        System.out.println(
                "Title: " + exception.getProblemDetail().getTitle()); // "Payment Required"
        System.out.println(
                "Balance: "
                        + exception.getProblemDetail().getProperties().get("balanceCents")); // 500L
    }
}
