package lab.springboot.questions;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SuppressWarnings("unused")
public final class Q25AutoConfigOrderingRulesExample {
    private Q25AutoConfigOrderingRulesExample() {}

    public static class MetricRegistry {}
    public static class UpstreamDataSourceAutoConfiguration {}

    // Auto-configuration ordering contract:
    // In Spring Boot 3+, @AutoConfiguration replaces @Configuration in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
    // 'after' guarantees UpstreamDataSourceAutoConfiguration executes first, so any beans it registers are visible to @ConditionalOnMissingBean.
    @AutoConfiguration(after = UpstreamDataSourceAutoConfiguration.class)
    @ConditionalOnClass(MetricRegistry.class)
    public static class CustomMetricsAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MetricRegistry defaultMetricRegistry() {
            return new MetricRegistry();
        }
    }

    public static void main(String[] args) {
        CustomMetricsAutoConfiguration config = new CustomMetricsAutoConfiguration();
        MetricRegistry registry = config.defaultMetricRegistry();
        boolean created = registry != null; // true
    }
}
