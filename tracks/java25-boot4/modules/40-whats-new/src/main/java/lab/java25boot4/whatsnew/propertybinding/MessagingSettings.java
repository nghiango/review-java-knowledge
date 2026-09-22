package lab.java25boot4.whatsnew.propertybinding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Outbound notification settings, bound and validated at startup.
 *
 * <p>Decision: a record with constructor binding, {@code @Validated} and Jakarta constraints. If a
 * Boot 4 key no longer matches a component — the failure this class exists to prevent — the
 * application fails to start with a binding error instead of running with {@code null} values.
 *
 * <p>Trade-off: every key becomes mandatory. Genuinely optional settings must be modelled
 * explicitly (an {@code Optional} component or a default) rather than by leaving validation off.
 */
@Validated
@ConfigurationProperties(prefix = "notifications.outbound")
public record MessagingSettings(
        @NotBlank String endpoint, @NotBlank String topic, @Positive int batchSize) {}
