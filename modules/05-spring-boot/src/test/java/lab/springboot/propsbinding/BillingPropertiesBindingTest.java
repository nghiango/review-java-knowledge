package lab.springboot.propsbinding;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingPropertiesBindingTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("Valid properties should have zero constraint violations")
    void validBillingProperties_passesValidation() {
        BillingProperties props = new BillingProperties(1.5, "USD", 10.0);
        Set<ConstraintViolation<BillingProperties>> violations = validator.validate(props);

        assertThat(violations).isEmpty();

        BillingConfigConsumer consumer = new BillingConfigConsumer(props);
        double charge = consumer.calculateCharge(100.0);
        assertThat(charge).isCloseTo(165.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Negative rate should violate @Positive constraint")
    void negativeRate_failsValidation() {
        BillingProperties props = new BillingProperties(-0.5, "USD", 10.0);
        Set<ConstraintViolation<BillingProperties>> violations = validator.validate(props);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("rate"));
    }

    @Test
    @DisplayName("Invalid currency code should violate @Pattern constraint")
    void invalidCurrency_failsValidation() {
        BillingProperties props = new BillingProperties(1.0, "us_dollar", 10.0);
        Set<ConstraintViolation<BillingProperties>> violations = validator.validate(props);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    @DisplayName("Tax percentage greater than 100 should violate @DecimalMax constraint")
    void excessiveTaxPercent_failsValidation() {
        BillingProperties props = new BillingProperties(1.0, "USD", 150.0);
        Set<ConstraintViolation<BillingProperties>> violations = validator.validate(props);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("taxPercent"));
    }
}
