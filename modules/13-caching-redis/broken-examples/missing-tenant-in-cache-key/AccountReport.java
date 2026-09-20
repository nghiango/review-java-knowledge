package lab.cachingredis.broken.multitenant;

import java.math.BigDecimal;

public record AccountReport(String reportId, String tenantId, String title, BigDecimal totalRevenue) {}
