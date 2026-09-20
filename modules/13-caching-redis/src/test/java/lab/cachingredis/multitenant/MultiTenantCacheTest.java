package lab.cachingredis.multitenant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class MultiTenantCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        SafeTenantReportService safeTenantReportService() {
            return new SafeTenantReportService();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(SafeTenantReportService.CACHE_NAME);
        }
    }

    private AnnotationConfigApplicationContext context;
    private SafeTenantReportService service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        service = context.getBean(SafeTenantReportService.class);
    }

    @Test
    @DisplayName("Tenants accessing the same report ID get isolated cache entries without leakage")
    void differentTenants_getIsolatedCacheEntries() {
        // Tenant Alpha fetches report "rep-01"
        AccountReport alphaReport = service.getReport("tenant-alpha", "rep-01");
        assertThat(alphaReport.tenantId()).isEqualTo("tenant-alpha");
        assertThat(alphaReport.totalRevenue()).isEqualByComparingTo("500000.00");

        // Tenant Beta fetches the SAME report ID "rep-01"
        AccountReport betaReport = service.getReport("tenant-beta", "rep-01");
        assertThat(betaReport.tenantId()).isEqualTo("tenant-beta");
        assertThat(betaReport.totalRevenue()).isEqualByComparingTo("12000.00");

        // Both calls should have executed storage reads (2 reads total, no cross-tenant cache hit)
        assertThat(service.getStorageReadCount()).isEqualTo(2);

        // Second call for Alpha should hit Alpha's cache entry
        AccountReport alphaCached = service.getReport("tenant-alpha", "rep-01");
        assertThat(alphaCached.totalRevenue()).isEqualByComparingTo("500000.00");
        assertThat(service.getStorageReadCount()).isEqualTo(2); // Read count unchanged!
    }

    @Test
    @DisplayName("Invalidating one tenant's report does not evict another tenant's cache entry")
    void invalidateReport_onlyEvictsTargetTenant() {
        service.getReport("tenant-alpha", "rep-01");
        service.getReport("tenant-beta", "rep-01");
        assertThat(service.getStorageReadCount()).isEqualTo(2);

        // Invalidate only Alpha's cache
        service.invalidateReport("tenant-alpha", "rep-01");

        // Reading Beta should still hit Beta's cache
        service.getReport("tenant-beta", "rep-01");
        assertThat(service.getStorageReadCount()).isEqualTo(2);

        // Reading Alpha must re-fetch from storage
        service.getReport("tenant-alpha", "rep-01");
        assertThat(service.getStorageReadCount()).isEqualTo(3);
    }
}
