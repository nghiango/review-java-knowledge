package lab.cachingredis.multitenant;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class SafeTenantReportService {

    public static final String CACHE_NAME = "tenantReports";

    private final Map<String, AccountReport> reportStorage = new ConcurrentHashMap<>();
    private final AtomicInteger storageReadCount = new AtomicInteger();

    public SafeTenantReportService() {
        reportStorage.put(
                "tenant-alpha:rep-01",
                new AccountReport(
                        "rep-01", "tenant-alpha", "Alpha Q1 Report", new BigDecimal("500000.00")));
        reportStorage.put(
                "tenant-beta:rep-01",
                new AccountReport(
                        "rep-01", "tenant-beta", "Beta Q1 Report", new BigDecimal("12000.00")));
    }

    // Multi-Tenant Key Scoping:
    // Every cache key MUST be namespaced with the tenant identifier (#tenantId + ':' + #reportId).
    // This strictly prevents cross-tenant data collisions where tenant-beta would receive
    // tenant-alpha's cached records.
    @Cacheable(value = CACHE_NAME, key = "#tenantId + ':' + #reportId")
    public AccountReport getReport(String tenantId, String reportId) {
        storageReadCount.incrementAndGet();
        String compositeStorageKey = tenantId + ":" + reportId;
        AccountReport report = reportStorage.get(compositeStorageKey);
        if (report == null) {
            throw new IllegalArgumentException("Report not found for tenant: " + tenantId);
        }
        return report;
    }

    // Eviction must also include the exact tenant namespace to prevent evicting another tenant's
    // cache entry
    @CacheEvict(value = CACHE_NAME, key = "#tenantId + ':' + #reportId")
    public void invalidateReport(String tenantId, String reportId) {
        // Eviction triggered via annotation
    }

    public int getStorageReadCount() {
        return storageReadCount.get();
    }
}
