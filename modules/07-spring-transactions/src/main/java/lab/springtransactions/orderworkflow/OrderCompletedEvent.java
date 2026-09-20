package lab.springtransactions.orderworkflow;

public record OrderCompletedEvent(
        String orderId, String customerEmail, double amount, String transactionRef) {}
