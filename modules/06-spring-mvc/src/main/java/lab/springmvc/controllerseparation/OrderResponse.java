package lab.springmvc.controllerseparation;

public record OrderResponse(String orderId, String customerId, double totalAmount, String status) {}
