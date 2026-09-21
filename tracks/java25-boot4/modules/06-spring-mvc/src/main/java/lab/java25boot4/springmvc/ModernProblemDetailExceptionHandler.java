package lab.java25boot4.springmvc;

import java.net.URI;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Modern Spring MVC RFC 9457 ProblemDetail advice with JSpecify null-safety. */
@RestControllerAdvice
public class ModernProblemDetailExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request Argument");
        problem.setType(URI.create("https://api.example.com/errors/invalid-argument"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Constraint Violation");
        problem.setType(URI.create("https://api.example.com/errors/validation-failed"));
        problem.setProperty("timestamp", Instant.now());

        var errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                err ->
                                        err.getField()
                                                + ": "
                                                + (err.getDefaultMessage() != null
                                                        ? err.getDefaultMessage()
                                                        : "invalid"))
                        .toList();
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
