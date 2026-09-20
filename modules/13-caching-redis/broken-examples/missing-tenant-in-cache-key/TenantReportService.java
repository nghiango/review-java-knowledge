package lab.cachingredis.broken.multitenant;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TenantReportService {

    private final Map<String, AccountReport> reportStorage = new ConcurrentHashMap<>();

    public TenantReportService() {
        reportStorage.put(
                "tenant-alpha:rep-01",
                new AccountReport("rep-01", "tenant-alpha", "Q1 Financials", new BigDecimal("500000.00")));
        reportStorage.put(
                "tenant-beta:rep-01",
                new AccountReport("rep-01", "tenant-beta", "Q1 Financials", new BigDecimal("12000.00")));
    }

    @Cacheable(value = "tenantReports", key = "#reportId")
    public AccountReport getReport(String tenantId, String reportId) {
        String compositeStorageKey = tenantId + ":" + reportId;
        AccountReport report = reportStorage.get(compositeStorageKey);
        if (report == null) {
            throw new IllegalArgumentException("Report not found for tenant: " + tenantId);
        }
        return report;
    }
}
