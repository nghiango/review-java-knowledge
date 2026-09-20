package lab.resilience.idempotentretry;

public record ChargeRequest(String customerId, double amount, String currency) {}
