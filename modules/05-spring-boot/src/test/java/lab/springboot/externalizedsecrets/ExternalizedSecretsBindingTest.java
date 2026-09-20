package lab.springboot.externalizedsecrets;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExternalizedSecretsBindingTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Valid externalized secrets should pass validation")
    void validSecrets_passesValidation() {
        PaymentGatewayProperties props =
                new PaymentGatewayProperties(
                        "https://api.payments.company.com", "sk_live_secret12345");
        Set<ConstraintViolation<PaymentGatewayProperties>> violations = validator.validate(props);

        assertThat(violations).isEmpty();

        PaymentGatewayClient client = new PaymentGatewayClient(props);
        assertThat(client.getApiKey()).isEqualTo("sk_live_secret12345");
        assertThat(client.getEndpointUrl()).isEqualTo("https://api.payments.company.com");
    }

    @Test
    @DisplayName("Blank API key should fail validation")
    void blankApiKey_failsValidation() {
        PaymentGatewayProperties props =
                new PaymentGatewayProperties("https://api.payments.company.com", "   ");
        Set<ConstraintViolation<PaymentGatewayProperties>> violations = validator.validate(props);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("apiKey"));
    }

    @Test
    @DisplayName("Blank endpoint URL should fail validation")
    void blankEndpointUrl_failsValidation() {
        PaymentGatewayProperties props = new PaymentGatewayProperties("", "sk_live_secret12345");
        Set<ConstraintViolation<PaymentGatewayProperties>> violations = validator.validate(props);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("endpointUrl"));
    }
}
