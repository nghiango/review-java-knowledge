package lab.restapi.idempotency;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "Account ID is required") String accountId,
        @NotNull(message = "Amount is required")
                @DecimalMin(value = "0.01", message = "Amount must be positive")
                BigDecimal amount,
        @NotBlank(message = "Currency is required") String currency) {}
