package lab.resilience.circuitbreaker;

public record CustomerRiskProfile(String customerId, int creditScore, String tier) {}
