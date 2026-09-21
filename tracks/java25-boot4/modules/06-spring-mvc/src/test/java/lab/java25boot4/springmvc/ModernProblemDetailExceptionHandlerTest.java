package lab.java25boot4.springmvc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class ModernProblemDetailExceptionHandlerTest {

    private final ModernProblemDetailExceptionHandler handler =
            new ModernProblemDetailExceptionHandler();

    @Test
    @DisplayName("Should format IllegalArgumentException as RFC 9457 ProblemDetail")
    void handleIllegalArgument_returnsProblemDetail() {
        var ex = new IllegalArgumentException("Version 0.9 is obsolete");
        ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Invalid Request Argument");
        assertThat(response.getBody().getDetail()).isEqualTo("Version 0.9 is obsolete");
        assertThat(response.getBody().getProperties()).containsKey("timestamp");
    }
}
