# Spring Boot Code Review Practice

Practice identifying configuration, security, concurrency, and architecture anti-patterns in real-world Spring Boot pull requests.

---

## Exercise 1: Unsecured Actuator Exposure

Review the following Actuator configuration and application configuration file.

### Code Under Review

```yaml
--8<-- "modules/05-spring-boot/broken-examples/unsecured-actuator-exposure/application.yml"
```

```java
--8<-- "modules/05-spring-boot/broken-examples/unsecured-actuator-exposure/ActuatorSecurityConfig.java"
```

### Review Questions
1. Which Actuator endpoints are exposed over HTTP?
2. What sensitive information is leaked by `/actuator/env` with `show-values: always`?
3. What availability risk is introduced by `shutdown: enabled: true`?

??? question "Reveal Issues & Analysis"

    - **Security Issue**: `include: "*"` exposes sensitive operational endpoints (`env`, `heapdump`, `configprops`) without authentication.
    - **Security Issue**: `show-values: always` displays plaintext passwords and API keys.
    - **Reliability Issue**: `shutdown: enabled: true` allows unauthenticated callers to stop the application remotely.
    - **Fix**: Expose only `health`, `info`, and `metrics`, disable remote shutdown, and isolate management endpoints to an internal private port.

---

## Exercise 2: Scattered `@Value` Configuration

Review the following billing and notification components that inject properties directly via `@Value`.

### Code Under Review

```java
--8<-- "modules/05-spring-boot/broken-examples/scattered-value-config/BillingConfigConsumer.java"
```

```java
--8<-- "modules/05-spring-boot/broken-examples/scattered-value-config/PaymentNotificationService.java"
```

### Review Questions
1. What happens if `app.billing.rate` is configured as a negative number or `tax-percent` exceeds 100?
2. How are fallback defaults managed across multiple consuming services?
3. Why is `@ConfigurationProperties` preferred here?

??? question "Reveal Issues & Analysis"

    - **Maintainability Issue**: Scattered raw property keys lack centralized ownership and IDE autocomplete.
    - **Validation Issue**: Missing `@Positive` and `@Min`/`@Max` validation allows invalid billing rates and tax percentages without failing fast on startup.
    - **Code Smell**: Duplicated fallback default `"USD"` across multiple classes leads to inconsistent configuration if defaults change.
    - **Fix**: Encapsulate properties in an immutable `@ConfigurationProperties(prefix = "app.billing")` record with `@Validated` and Bean Validation annotations.

---

## Exercise 3: Production Secrets Committed in Repository

Review the following production configuration profile and client component.

### Code Under Review

```yaml
--8<-- "modules/05-spring-boot/broken-examples/profile-secrets-in-repo/application-prod.yml"
```

```java
--8<-- "modules/05-spring-boot/broken-examples/profile-secrets-in-repo/PaymentGatewayClient.java"
```

### Review Questions
1. Where are production database credentials and API keys stored?
2. What are the security risks of committing production profiles to version control?
3. How should secrets be provided in a cloud deployment?

??? question "Reveal Issues & Analysis"

    - **Security Issue**: Hardcoded production API keys and plaintext database passwords committed to VCS history leak credentials.
    - **Maintainability Issue**: Couples deployment credentials to application source code.
    - **Fix**: Use environment variable placeholder references (e.g. `api-key: "${PAYMENT_GATEWAY_API_KEY}"`) with `@NotBlank` validation, injecting actual secrets at runtime via Kubernetes Secrets or a secret manager.

---

## Exercise 4: No Graceful Shutdown with In-Flight Work

Review the following batch task processor and server configuration.

### Code Under Review

```java
--8<-- "modules/05-spring-boot/broken-examples/no-graceful-shutdown/BatchTaskProcessor.java"
```

```yaml
--8<-- "modules/05-spring-boot/broken-examples/no-graceful-shutdown/application.yml"
```

### Review Questions
1. What happens to running batch jobs when the container receives a `SIGTERM`?
2. Why does the raw `ExecutorService` fail to participate in Spring shutdown lifecycle?
3. What is the impact of `server.shutdown: immediate` during rolling deployments?

??? question "Reveal Issues & Analysis"

    - **Concurrency Issue**: Raw `ExecutorService` created via `Executors.newFixedThreadPool(4)` does not participate in Spring `SmartLifecycle` or bean destruction callbacks.
    - **Reliability Issue**: Tasks in flight are abruptly aborted when the JVM terminates.
    - **Reliability Issue**: `server.shutdown: immediate` causes HTTP connection resets and 502 errors.
    - **Fix**: Configure `server.shutdown: graceful` and manage worker threads via Spring's `ThreadPoolTaskExecutor` with `setWaitForTasksToCompleteOnShutdown(true)` and `setAwaitTerminationSeconds(30)`.

---

## Exercise 5: Accidental Auto-Configuration Override

Review the following custom client configuration and service.

### Code Under Review

```java
--8<-- "modules/05-spring-boot/broken-examples/accidental-autoconfig-override/CustomRestClientConfig.java"
```

```java
--8<-- "modules/05-spring-boot/broken-examples/accidental-autoconfig-override/ExternalPaymentClient.java"
```

```java
--8<-- "modules/05-spring-boot/broken-examples/accidental-autoconfig-override/PaymentProcessingService.java"
```

### Review Questions
1. How does declaring a raw `@Bean` impact Spring Boot auto-configured features (tracing, metrics, connection pools)?
2. How should custom configurations extend framework defaults without replacing them?

??? question "Reveal Issues & Analysis"

    - **Maintainability Issue**: Defining a raw bean directly overrides framework auto-configuration without applying customizers.
    - **Observability Issue**: Bypasses auto-configured Micrometer observation, tracing propagation, and metric collection.
    - **Reliability Issue**: Lacks connection pooling and timeout configuration provided by standard auto-configuration builders.
    - **Fix**: Use `@ConditionalOnMissingBean` on auto-configured bean definitions and supply customizer interfaces (e.g. `ClientCustomizer`) for non-intrusive customization.
