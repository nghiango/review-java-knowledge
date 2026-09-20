package lab.restapi.problemdetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ProblemDetailRestExceptionHandlerTest {

    private SafeCustomerController controller;
    private GlobalRestExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        controller = new SafeCustomerController();
        exceptionHandler = new GlobalRestExceptionHandler();
    }

    @Test
    @DisplayName("Handle CustomerNotFoundException returns 404 RFC 9457 ProblemDetail")
    void handleCustomerNotFound_formatsProblemDetail() {
        UUID randomId = UUID.randomUUID();
        CustomerNotFoundException ex = new CustomerNotFoundException(randomId);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/customers/" + randomId);

        ProblemDetail problem =
                exceptionHandler.handleCustomerNotFound(ex, new ServletWebRequest(request));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Customer Not Found");
        assertThat(problem.getDetail()).contains(randomId.toString());
        assertThat(problem.getProperties()).containsKey("customerId");
    }

    @Test
    @DisplayName(
            "Handle InsufficientCreditException returns 422 Unprocessable Entity ProblemDetail")
    void handleInsufficientCredit_formatsProblemDetail() {
        UUID id = UUID.randomUUID();
        InsufficientCreditException ex =
                new InsufficientCreditException(
                        id, new BigDecimal("10.00"), new BigDecimal("50.00"));
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/customers/" + id + "/charge");

        ProblemDetail problem =
                exceptionHandler.handleInsufficientCredit(ex, new ServletWebRequest(request));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getTitle()).isEqualTo("Insufficient Credit");
        assertThat(problem.getProperties()).containsEntry("customerId", id);
        assertThat(problem.getProperties())
                .containsEntry("currentBalance", new BigDecimal("10.00"));
        assertThat(problem.getProperties())
                .containsEntry("requiredAmount", new BigDecimal("50.00"));
    }

    @Test
    @DisplayName(
            "SafeCustomerController throws InsufficientCreditException when balance is too low")
    void chargeCustomer_insufficientBalance_throwsException() {
        UUID defaultId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // has 50.00
        assertThatThrownBy(
                        () ->
                                controller.chargeCustomer(
                                        defaultId,
                                        new SafeCustomerController.ChargeRequest(
                                                new BigDecimal("100.00"))))
                .isInstanceOf(InsufficientCreditException.class);
    }
}
