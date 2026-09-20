package lab.springtransactions.asynctransaction;

public record OrderCreatedEvent(String orderId, String customerEmail, double amount) {}
