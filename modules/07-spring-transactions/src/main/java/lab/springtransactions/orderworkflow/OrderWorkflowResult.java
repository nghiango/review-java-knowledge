package lab.springtransactions.orderworkflow;

public record OrderWorkflowResult(
        String orderId, String status, String transactionRef, String message) {}
