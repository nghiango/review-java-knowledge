# Spring Boot Hands-on Exercises

Practical coding and configuration challenges to solidify Spring Boot architecture and operational knowledge.

---

## Exercise 1: Build a Type-Safe Rate Limiter Configuration

### Problem Statement
Create a configuration record `RateLimiterProperties` prefixed with `app.rate-limiter` that binds:
1. `maxRequestsPerMinute` (integer, must be positive, min 1, max 100,000).
2. `clientHeader` (non-blank string, e.g. `"X-API-KEY"`).
3. `whitelistedIps` (list of valid IP strings, cannot be null).

Add `@Validated` and write a unit test verifying that negative request limits fail fast during startup.

??? question "Reveal Solution"

    ```java
    package lab.springboot.propsbinding;

    import jakarta.validation.constraints.Max;
    import jakarta.validation.constraints.Min;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.NotNull;
    import jakarta.validation.constraints.Positive;
    import java.util.List;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.validation.annotation.Validated;

    @ConfigurationProperties(prefix = "app.rate-limiter")
    @Validated
    public record RateLimiterProperties(
            @NotNull @Positive @Min(1) @Max(100000) Integer maxRequestsPerMinute,
            @NotBlank String clientHeader,
            @NotNull List<String> whitelistedIps) {}
    ```

---

## Exercise 2: Implement a Conditional Custom Health Indicator

### Problem Statement
Implement a custom Spring Boot Actuator `HealthIndicator` named `DiskQuotaHealthIndicator` that:
1. Is conditionally registered only when `management.health.diskquota.enabled=true`.
2. Checks available disk space and returns `Health.down()` if free disk space is less than 500 MB.

??? question "Reveal Solution"

    ```java
    package lab.springboot.actuatorsecurity;

    import java.io.File;
    import org.springframework.boot.actuate.health.Health;
    import org.springframework.boot.actuate.health.HealthIndicator;
    import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
    import org.springframework.stereotype.Component;

    @Component
    @ConditionalOnProperty(prefix = "management.health.diskquota", name = "enabled", havingValue = "true", matchIfMissing = false)
    public class DiskQuotaHealthIndicator implements HealthIndicator {

        private static final long MIN_FREE_BYTES = 500L * 1024 * 1024; // 500MB

        @Override
        public Health health() {
            File root = new File(".");
            long freeSpace = root.getFreeSpace();

            if (freeSpace < MIN_FREE_BYTES) {
                return Health.down()
                        .withDetail("freeSpaceBytes", freeSpace)
                        .withDetail("requiredBytes", MIN_FREE_BYTES)
                        .withDetail("reason", "Low disk space threshold violated")
                        .build();
            }

            return Health.up()
                    .withDetail("freeSpaceBytes", freeSpace)
                    .build();
        }
    }
    ```

---

## Exercise 3: Zero-Downtime Graceful Shutdown Verification

### Problem Statement
Configure a Spring Boot application running on Kubernetes so that rolling updates achieve zero dropped connections. Describe the required `application.yml` parameters and Kubernetes pod spec lifecycle hooks.

??? question "Reveal Solution"

    **1. `application.yml`:**
    ```yaml
    server:
      shutdown: graceful

    spring:
      lifecycle:
        timeout-per-shutdown-phase: 25s
    ```

    **2. Kubernetes Deployment Spec:**
    ```yaml
    spec:
      template:
        spec:
          terminationGracePeriodSeconds: 40
          containers:
            - name: app
              lifecycle:
                preStop:
                  exec:
                    command: ["/bin/sh", "-c", "sleep 10"]
    ```

    **Explanation**:
    The `preStop` hook sleeps for 10 seconds while kube-proxy updates iptables/ipvs endpoints. Then `SIGTERM` is sent, and Spring Boot gracefully finishes active requests within 25 seconds before the total 40-second pod termination grace period expires.
