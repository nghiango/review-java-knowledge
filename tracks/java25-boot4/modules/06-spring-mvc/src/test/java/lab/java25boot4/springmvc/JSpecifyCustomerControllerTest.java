package lab.java25boot4.springmvc;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class JSpecifyCustomerControllerTest {

    private final JSpecifyCustomerController controller = new JSpecifyCustomerController();
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Should return customer profile with null phone and fallback company")
    void getCustomer_withFallbackCompany_returnsProfile() {
        ResponseEntity<JSpecifyCustomerController.CustomerProfile> response =
                controller.getCustomer("CUST-1", "Acme-Global");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo("CUST-1");
        assertThat(response.getBody().phone()).isNull();
        assertThat(response.getBody().company()).isEqualTo("Acme-Global");
    }

    @Test
    @DisplayName("Should update customer profile successfully")
    void updateCustomer_validPayload_returnsUpdatedProfile() {
        var request = new JSpecifyCustomerController.UpdateCustomerRequest("Alice", "555-0199");
        var violations = validator.validate(request);
        assertThat(violations).isEmpty();

        ResponseEntity<JSpecifyCustomerController.CustomerProfile> response =
                controller.updateCustomer("CUST-2", request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo("CUST-2");
        assertThat(response.getBody().name()).isEqualTo("Alice");
        assertThat(response.getBody().phone()).isEqualTo("555-0199");
        assertThat(response.getBody().company()).isEqualTo("Corp-555-0199");
    }

    @Test
    @DisplayName("Should violate @NotBlank when update customer name is blank")
    void updateCustomer_blankName_failsValidation() {
        var request = new JSpecifyCustomerController.UpdateCustomerRequest("", "555-0199");
        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }
}
