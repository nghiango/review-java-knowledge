package lab.restapi.broken.problemdetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LegacyErrorHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));

        // Leaks complete Java exception class name, message, and stack trace in HTTP 500 response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        Map.of(
                                "exception", ex.getClass().getName(),
                                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                                "stackTrace", sw.toString()));
    }
}
