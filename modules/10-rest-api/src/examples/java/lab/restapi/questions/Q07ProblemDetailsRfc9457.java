package lab.restapi.questions;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public class Q07ProblemDetailsRfc9457 {

    public static void main(String[] args) {
        // Construct RFC 9457 ProblemDetail
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "The requested withdrawal exceeds available balance.");

        problem.setType(URI.create("https://api.bank.com/errors/insufficient-funds"));
        problem.setTitle("Insufficient Funds");
        problem.setInstance(URI.create("/api/accounts/acc-100/withdraw"));
        problem.setProperty("currentBalance", 45.50);
        problem.setProperty("requestedAmount", 100.00);

        int status = problem.getStatus(); // 400
        String title = problem.getTitle(); // "Insufficient Funds"
        Object currentBalance = problem.getProperties().get("currentBalance"); // 45.5

        System.out.println("Status: " + status); // Status: 400
        System.out.println("Title: " + title); // Title: Insufficient Funds
        System.out.println("Custom prop: " + currentBalance); // Custom prop: 45.5
    }
}
