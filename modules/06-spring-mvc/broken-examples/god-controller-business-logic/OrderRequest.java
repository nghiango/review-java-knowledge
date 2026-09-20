package lab.springmvc.broken.controllerseparation;

public record OrderRequest(
        String customerId,
        String productId,
        int quantity,
        double unitPrice,
        String paymentMethod) {}
