# Spring Boot Reference Solutions & Trade-Offs

Production-grade solutions corresponding to the code review exercises, detailing implementation choices and architectural trade-offs.

---

## Solution 1: Secure Actuator Endpoint Configuration

### Implementation

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/actuatorsecurity/ActuatorSecurityConfig.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/actuatorsecurity/HealthGroupConfig.java"
```

### Why This Works
1. **Explicit Endpoint Whitelist**: Only safe operational endpoints (`health`, `info`, `metrics`) are exposed over HTTP. Wildcard `include: "*"` is strictly prohibited.
2. **Health Group Segmentation**: Kubernetes probes target dedicated health groups (`/actuator/health/liveness` and `/actuator/health/readiness`), preventing external database connectivity issues from triggering pod restarts on liveness probes.
3. **Environment Value Protection**: Sensitive environment properties are masked (`show-values: when_authorized` or `never`), preventing credential leaks.

### Trade-Offs
- **Security vs Observability Convenience**: Developers cannot inspect live environment properties via public HTTP endpoints; debugging production configuration requires authorized access or log aggregation.

---

## Solution 2: Type-Safe Configuration Properties Binding

### Implementation

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/propsbinding/BillingProperties.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/propsbinding/BillingConfigConsumer.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/propsbinding/PaymentNotificationService.java"
```

### Why This Works
1. **Immutable Records & Constructor Binding**: Properties are validated and instantiated as immutable records at startup, preventing runtime mutation.
2. **Startup Fail-Fast Validation**: Jakarta Bean Validation annotations (`@NotNull`, `@Positive`, `@DecimalMin`, `@DecimalMax`, `@Pattern`) validate properties during `ApplicationContext` initialization, aborting startup immediately if configuration is malformed.
3. **Single Source of Truth**: Eliminates duplicate `@Value` property strings across services.

### Trade-Offs
- **Strictness vs Dynamic Reloading**: Immutable record properties require an application restart or Spring Cloud Bus refresh to update values, which is preferred for stability in containerized microservices.

---

## Solution 3: Externalized Secret Management

### Implementation

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/externalizedsecrets/PaymentGatewayProperties.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/externalizedsecrets/PaymentGatewayClient.java"
```

### Why This Works
1. **Decoupling Secrets from Source Code**: Configuration profile files contain environment variable placeholders without hardcoded credentials.
2. **Enforced Non-Blank Validation**: `@NotBlank` guarantees that if a required secret environment variable (e.g. `PAYMENT_GATEWAY_API_KEY`) is missing at runtime, the application fails to start with a clear error.

### Trade-Offs
- **Operational Discipline**: Requires deployment manifests (Kubernetes Secrets, Terraform, AWS Secrets Manager) to provide necessary environment variables during container orchestration.

---

## Solution 4: Graceful Shutdown Coordination

### Implementation

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/gracefulshutdown/ShutdownConfig.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/gracefulshutdown/GracefulTaskProcessor.java"
```

### Why This Works
1. **Lifecycle Integration**: Worker threads are managed via Spring's `ThreadPoolTaskExecutor`, which participates in `SmartLifecycle` phase callbacks.
2. **Task Drain & Await Termination**: `setWaitForTasksToCompleteOnShutdown(true)` and `setAwaitTerminationSeconds(30)` ensure in-flight jobs are allowed to complete before the JVM exits.
3. **Zero 502 Bad Gateway Errors**: When combined with `server.shutdown: graceful`, the embedded web server stops accepting new connections and finishes active HTTP requests.

### Trade-Offs
- **Deployment Duration**: Rolling deployments may take slightly longer because terminating pods wait up to the timeout window to drain active tasks before stopping.

---

## Solution 5: Non-Intrusive Auto-Configuration Extension

### Implementation

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/autoconfigoverride/PaymentClientProperties.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/autoconfigoverride/PaymentClientAutoConfiguration.java"
```

```java
--8<-- "modules/05-spring-boot/src/main/java/lab/springboot/autoconfigoverride/PaymentProcessingService.java"
```

### Why This Works
1. **`@ConditionalOnMissingBean` Back-Off**: Auto-configuration automatically provides production defaults (metrics, tracing, connection pools) unless the user explicitly declares a custom bean.
2. **Properties-Driven Customization**: Common customizations (e.g. `payment.client.base-url`) are handled via `@ConfigurationProperties` without needing custom bean definitions.

### Trade-Offs
- **Condition Ordering Awareness**: Auto-configuration requires proper `@AutoConfigureBefore` / `@AutoConfigureAfter` ordering to ensure conditional evaluations see user-defined beans in the right phase.
