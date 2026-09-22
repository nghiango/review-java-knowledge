package lab.java25boot4.whatsnew.propertybinding;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class MessagingSettingsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validSettings_passValidation() {
        MessagingSettings settings = new MessagingSettings("http://broker", "orders", 100);

        assertThat(validator.validate(settings)).isEmpty();
    }

    @Test
    void blankTopic_failsValidation() {
        MessagingSettings settings = new MessagingSettings("http://broker", "", 100);

        assertThat(validator.validate(settings))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("topic"));
    }

    @Test
    void context_bindsEveryKey() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues(
                        "notifications.outbound.endpoint=http://broker",
                        "notifications.outbound.topic=orders",
                        "notifications.outbound.batch-size=50")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            MessagingSettings settings = context.getBean(MessagingSettings.class);
                            assertThat(settings.topic()).isEqualTo("orders");
                            assertThat(settings.endpoint()).isEqualTo("http://broker");
                            assertThat(settings.batchSize()).isEqualTo(50);
                        });
    }

    @Test
    void context_renamedKey_failsFastInsteadOfDroppingMessages() {
        new ApplicationContextRunner()
                .withUserConfiguration(BindingConfiguration.class)
                .withPropertyValues(
                        "notifications.outbound.endpoint=http://broker",
                        "notifications.outbound.destination=orders",
                        "notifications.outbound.batch-size=50")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(MessagingSettings.class)
    static class BindingConfiguration {}
}
