package lab.springmvc.exceptionhandling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName(
            "OrderNotFoundException should map to HTTP 404 ProblemDetail with orderId property")
    void handleOrderNotFound_returns404ProblemDetail() {
        OrderNotFoundException ex = new OrderNotFoundException("ord-999");
        ProblemDetail problem = handler.handleOrderNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Order Not Found");
        assertThat(problem.getDetail()).contains("ord-999");
        assertThat(problem.getProperties()).containsEntry("orderId", "ord-999");
    }

    @Test
    @DisplayName("Unhandled exception should map to HTTP 500 ProblemDetail with sanitized message")
    void handleUnhandledException_returnsSanitized500ProblemDetail() {
        Exception ex = new IllegalStateException("Internal DB secret connection string leak");
        ProblemDetail problem = handler.handleUnhandledException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).doesNotContain("Internal DB secret");
        assertThat(problem.getDetail()).contains("unexpected internal error");
    }
}
