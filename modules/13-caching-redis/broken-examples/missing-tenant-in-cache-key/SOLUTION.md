# Solution — Cross-Tenant Cache Leakage

## Annotated code

```java
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

    // Security issue: Cross-tenant data leakage. The cache key includes only #reportId without
    // namespacing by #tenantId. When Tenant A accesses report "rep-01", it is cached under key "rep-01".
    // When Tenant B subsequently requests report "rep-01", the cache returns Tenant A's private
    // financial record to Tenant B (CWE-639 / Insecure Direct Object Reference).
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
```

## Issues identified

### 1. Cross-tenant cache key collision and data breach
- **Category:** Security issue
- **Severity:** Critical
- **Explanation:** In multi-tenant systems, resources frequently share common business identifiers (e.g. `rep-01`, invoice `1`, account `default`). Configuring `@Cacheable(key = "#reportId")` ignores the tenant boundary. As a result, whichever tenant accesses a report ID first populates the shared cache, and all subsequent requests from any tenant receive that same cached object, causing severe data contamination and regulatory compliance violations (GDPR, SOC 2, HIPAA).

### 2. Missing compound key generator
- **Category:** Design issue
- **Severity:** High
- **Explanation:** Cache keys must always be scoped with tenant context, e.g. `key = "#tenantId + ':' + #reportId"` or via a custom `TenantAwareKeyGenerator` that injects the current tenant context automatically.

## Correct implementation

See `lab.cachingredis.multitenant.SafeTenantReportService` under `src/main/java`.

## Trade-offs

- **Compound SpEL Keys:** Simple to declare (`key = "#tenantId + ':' + #reportId"`), but easy for developers to accidentally omit on new methods.
- **TenantAwareKeyGenerator:** Automatically prepends tenant context across all `@Cacheable` methods globally, eliminating developer omission errors, but requires thread-local or reactive tenant context propagation.
