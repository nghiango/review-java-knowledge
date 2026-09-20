package lab.springtransactions.orderworkflow;

public record OrderWorkflowCommand(
        String orderId, String customerEmail, String accountId, double amount) {}
