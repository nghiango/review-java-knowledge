package lab.springboot.externalizedsecrets;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "payment.gateway")
@Validated
public record PaymentGatewayProperties(@NotBlank String endpointUrl, @NotBlank String apiKey) {}
