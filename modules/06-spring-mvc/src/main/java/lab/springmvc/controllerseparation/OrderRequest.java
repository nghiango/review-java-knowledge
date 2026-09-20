package lab.springmvc.controllerseparation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
        @NotBlank String customerId,
        @NotBlank String productId,
        @Positive int quantity,
        @Positive double unitPrice,
        @NotBlank String paymentMethod) {}
