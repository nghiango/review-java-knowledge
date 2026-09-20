package lab.concurrency.asyncpipeline;

import java.util.Map;

public record CustomerDashboard(String customerId, int totalOrders, Map<String, Double> prices) {}
