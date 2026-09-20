package lab.cachingredis.multitenant;

import java.math.BigDecimal;

public record AccountReport(
        String reportId, String tenantId, String title, BigDecimal totalRevenue) {}
