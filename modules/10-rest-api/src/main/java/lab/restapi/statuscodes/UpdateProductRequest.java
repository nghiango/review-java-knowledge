package lab.restapi.statuscodes;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Name must not be blank") String name,
        @NotNull(message = "Price is required")
                @DecimalMin(value = "0.01", message = "Price must be positive")
                BigDecimal price) {}
