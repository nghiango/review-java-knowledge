# Spring Boot Interview Questions

Categorized interview questions with model answers and compilable code examples.

---

## Basic Questions<!-- --8<-- [start:basic] -->

### Q1: What is the primary difference between Spring Framework and Spring Boot?

??? question "Reveal answer"

    **Short Answer:** Spring Framework provides core dependency injection, AOP, and web MVC abstractions but requires extensive manual boilerplate configuration. Spring Boot is an opinionated layer providing automatic configuration, dependency starters, embedded web servers, and production-ready operational telemetry (Actuator).

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q01SpringBootVsSpringFrameworkExample.java"
        ```

### Q2: What is the purpose of a Spring Boot Starter?

??? question "Reveal answer"

    **Short Answer:** A starter is a dependency aggregator POM/module that packages curated, version-managed transitive libraries for a specific capability (e.g. `spring-boot-starter-web`), eliminating manual dependency coordination and version conflicts.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q02StartersAnatomyExample.java"
        ```

### Q3: How does Spring Boot auto-configuration work?

??? question "Reveal answer"

    **Short Answer:** Auto-configuration evaluates conditional annotations (`@ConditionalOnClass`, `@ConditionalOnMissingBean`) against classpath libraries, existing bean definitions, and environment properties at startup to automatically register framework defaults.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q03AutoConfigurationMechanismExample.java"
        ```

### Q4: What are the most common `@Conditional` annotations in Spring Boot?

??? question "Reveal answer"

    **Short Answer:** `@ConditionalOnClass` (checks classpath), `@ConditionalOnMissingBean` (checks existing bean registrations), `@ConditionalOnProperty` (checks configuration values), `@ConditionalOnWebApplication` (checks web runtime), and `@ConditionalOnResource` (checks file presence).

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q04ConditionalAnnotationsExample.java"
        ```

### Q5: Why is `@ConfigurationProperties` preferred over `@Value`?

??? question "Reveal answer"

    **Short Answer:** `@ConfigurationProperties` provides structured hierarchical grouping, immutable record binding, relaxed binding across naming conventions, IDE auto-completion support, and startup fail-fast validation via Jakarta Bean Validation.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q05ConfigurationPropertiesBindingExample.java"
        ```

### Q6: What is the property resolution precedence in Spring Boot?

??? question "Reveal answer"

    **Short Answer:** Spring Boot follows a strict 17-level hierarchy where higher sources override lower ones: Command-line arguments $\to$ Java System properties $\to$ OS Environment variables $\to$ Profile-specific properties (`application-{profile}.yml`) $\to$ Default `application.yml`.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q06PropertyPrecedenceHierarchyExample.java"
        ```

### Q7: What are Spring Boot Actuator endpoints and why must they be secured?

??? question "Reveal answer"

    **Short Answer:** Actuator provides operational endpoints for health, metrics, tracing, and diagnostics. Endpoints like `/actuator/env`, `/actuator/heapdump`, and `/actuator/shutdown` expose sensitive credentials and remote termination capabilities, requiring strict network and authentication controls.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q07ActuatorEndpointsSecurityExample.java"
        ```

### Q8: What is graceful shutdown in Spring Boot?

??? question "Reveal answer"

    **Short Answer:** Graceful shutdown (`server.shutdown=graceful`) stops accepting new incoming HTTP connections on SIGTERM and allows currently in-flight requests to complete within a configured timeout window before stopping the embedded server.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q08GracefulShutdownLifecycleExample.java"
        ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Questions

### Q9: What are the lifecycle events published by `SpringApplication` during startup?

??? question "Reveal answer"

    **Short Answer:** `SpringApplication` publishes ordered events: `ApplicationStartingEvent` $\to$ `ApplicationEnvironmentPreparedEvent` $\to$ `ApplicationContextInitializedEvent` $\to$ `ApplicationPreparedEvent` $\to$ `ApplicationStartedEvent` $\to$ `ApplicationReadyEvent`.

    **Internal Mechanism:** `SpringApplicationRunListeners` broadcasts lifecycle milestones. Early events (starting, environment prepared) fire before the `ApplicationContext` is created; later events fire once beans are initialized and `CommandLineRunner` beans have completed.

    **Common Mistake:** Subscribing to `@EventListener` for `ApplicationStartingEvent` inside a standard `@Component` bean; because the bean does not exist yet when the event fires, the listener never receives the event (it must be registered in `META-INF/spring.factories` or via `SpringApplication.addListeners`).

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q09SpringApplicationStartupPhasesExample.java"
        ```

### Q10: How do `@AutoConfigureBefore` and `@AutoConfigureAfter` control auto-configuration ordering?

??? question "Reveal answer"

    **Short Answer:** They define topological dependencies between auto-configuration classes to ensure prerequisites (e.g. DataSource or Security) are registered before dependent configurations (e.g. JPA or Web Security).

    **Internal Mechanism:** `AutoConfigurationSorter` builds a directed dependency graph using `@AutoConfigureBefore`, `@AutoConfigureAfter`, and `@AutoConfigureOrder` to sort auto-configuration import candidates prior to condition evaluation. Standard `@Order` is ignored on auto-configuration classes.

    **Common Mistake:** Using `@Order` or `@Priority` on an `@AutoConfiguration` class expecting it to sequence before another auto-configuration class.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q10AutoConfigurationOrderingExample.java"
        ```

### Q11: How do you diagnose why a specific auto-configuration was or was not applied?

??? question "Reveal answer"

    **Short Answer:** Start the application with the `--debug` flag or inspect the `ConditionEvaluationReport` via the `/actuator/conditions` endpoint to view positive and negative condition matches.

    **Internal Mechanism:** `ConditionEvaluationReport` records every condition evaluation outcome (matched, not matched, exclusions) during the `AutoConfigurationImportSelector` and `ConditionEvaluator` passes.

    **Common Mistake:** Guessing missing dependencies without checking the condition report; looking at generic startup stack traces instead of the detailed evaluation breakdown.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q11ConditionEvaluationReportExample.java"
        ```

### Q12: What is the recommended multi-module structure for custom Spring Boot starters?

??? question "Reveal answer"

    **Short Answer:** Separate the project into two modules: an `acme-spring-boot-autoconfigure` module (containing `@AutoConfiguration` classes, conditional beans, and imports metadata) and an `acme-spring-boot-starter` module (an empty aggregator packaging the core library and autoconfigure module).

    **Internal Mechanism:** Separating autoconfigure from the starter allows users to import only the auto-configuration logic if they already manage core library versions independently, avoiding dependency bloat.

    **Common Mistake:** Placing business logic and library code directly inside the starter JAR rather than maintaining a pure dependency aggregator.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q12CustomStarterStructureExample.java"
        ```

### Q13: How does profile activation and multi-document YAML work in Spring Boot?

??? question "Reveal answer"

    **Short Answer:** Multi-document YAML files use `---` delimiters and `spring.config.activate.on-profile` to define profile-specific properties within a single file, supporting boolean profile expressions (e.g. `prod & cloud`).

    **Internal Mechanism:** `YamlPropertySourceLoader` parses YAML documents sequentially into distinct property sources, evaluating activation predicates against the active environment profiles.

    **Common Mistake:** Using deprecated `spring.profiles: prod` syntax instead of modern `spring.config.activate.on-profile: prod`, leading to unexpected configuration merging.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q13ProfileActivationMultiDocYamlExample.java"
        ```

### Q14: How do Actuator Health Groups map to Kubernetes liveness and readiness probes?

??? question "Reveal answer"

    **Short Answer:** Spring Boot separates health checks into `/actuator/health/liveness` (indicating whether the JVM process is alive and responsive) and `/actuator/health/readiness` (indicating whether the application can accept traffic, including DB and cache connectivity).

    **Internal Mechanism:** `HealthEndpointGroup` filters health indicators by group membership. In a Kubernetes environment, Spring Boot automatically enables `livenessState` and `readinessState` probe indicators based on `AvailabilityState`.

    **Common Mistake:** Including external database checks in the liveness probe; if the database experiences a momentary blip, Kubernetes restarts healthy application pods in a cascading crash loop.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q14ActuatorHealthGroupsProbesExample.java"
        ```

### Q15: How does `@ServiceConnection` improve integration testing with Testcontainers?

??? question "Reveal answer"

    **Short Answer:** `@ServiceConnection` automatically extracts dynamic host, port, and credential details from running Testcontainers instances and registers them as Spring Boot connection properties, replacing manual `@DynamicPropertySource` methods.

    **Internal Mechanism:** Spring Boot 3.1+ provides `ContainerConnectionDetailsFactory` SPI implementations that inspect container metadata (e.g. `PostgreSQLContainer`) and bind `ConnectionDetails` beans into the `ApplicationContext`.

    **Common Mistake:** Writing verbose `@DynamicPropertySource` methods that manually concatenate `container.getJdbcUrl()` strings for every test container.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q15ServiceConnectionTestcontainersExample.java"
        ```

### Q16: How does Spring Boot integrate with Java 21 Virtual Threads?

??? question "Reveal answer"

    **Short Answer:** Setting `spring.threads.virtual.enabled=true` configures embedded web servers (Tomcat, Jetty) and asynchronous task executors to use lightweight virtual threads per request/task.

    **Internal Mechanism:** Spring Boot auto-configures `TomcatProtocolHandlerCustomizer` and `TaskExecutorBuilder` to supply `Executors.newVirtualThreadPerTaskExecutor()`, enabling massive concurrency for I/O-bound workloads without thread pool exhaustion.

    **Common Mistake:** Running CPU-intensive computation or synchronized blocks with long-held locks on virtual threads, causing carrier thread pinning.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q16VirtualThreadsConfigurationExample.java"
        ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Questions

### Q17: How does Spring Boot Docker Compose integration streamline local development?

??? question "Reveal answer"

    **Short Answer:** The `spring-boot-docker-compose` module automatically discovers `compose.yaml`, starts required backing services on application startup, binds connection properties via `ConnectionDetails`, and stops containers on JVM exit.

    **Deep Explanation:** When added to the classpath in development, Spring Boot inspects the Docker Compose file, detects supported service images (e.g. Postgres, Redis, Kafka), validates their health status, and injects connection parameters without requiring manual environment variables.

    **Internal Mechanism:** `DockerComposeLifecycle` runs early in `SpringApplication` bootstrap, executing `docker compose up` and registering `DockerComposeConnectionDetails` for discovered services.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q17DockerComposeIntegrationExample.java"
        ```

    **Common Mistake:** Leaving `spring-boot-docker-compose` on the production runtime classpath, causing startup attempts to invoke Docker binaries in production containers.

    **Production Consideration:** Scope `spring-boot-docker-compose` to `developmentOnly` in Gradle so it is excluded from production archive artifacts.

    **Follow-up Questions:**
    - How does `spring.docker.compose.lifecycle-management` differ between `start-and-stop` and `start-only`?
    - How can you override specific container connection properties when using Docker Compose?

### Q18: What architectural guarantees does Spring Modulith provide to modular Spring Boot monoliths?

??? question "Reveal answer"

    **Short Answer:** Spring Modulith verifies logical module boundaries, enforces package encapsulation rules, and provides transactional event publication for loosely coupled internal domains.

    **Deep Explanation:** In standard Spring monoliths, developers frequently inject internal services across package boundaries, resulting in an unmaintainable "big ball of mud". Spring Modulith verifies architectural rules at test time (via ArchUnit) and ensures inter-module communication occurs via exposed APIs or domain events.

    **Internal Mechanism:** `ApplicationModules.of(Application.class).verify()` analyzes module dependencies and package visibility rules, failing tests if an unexposed package is referenced by another module.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q18SpringModulithArchitectureExample.java"
        ```

    **Common Mistake:** Allowing cross-module direct repository calls rather than publishing domain events or calling explicit module API contracts.

    **Production Consideration:** Use the Modulith Event Publication Registry to ensure asynchronous events are durably persisted and re-delivered upon failure.

    **Follow-up Questions:**
    - How does Spring Modulith generate C4 architecture diagrams and component documentation?
    - How does the transactional event publication registry guarantee at-least-once delivery?

### Q19: How do you implement a custom `FailureAnalyzer` for startup diagnostic clarity?

??? question "Reveal answer"

    **Short Answer:** Implement `AbstractFailureAnalyzer<T>` and register it in `META-INF/spring/org.springframework.boot.diagnostics.FailureAnalyzer.imports` to intercept specific startup exceptions and produce human-readable failure descriptions and corrective actions.

    **Deep Explanation:** When an exception crashes application startup, standard stack traces can be hundreds of lines long. A `FailureAnalyzer` catches the root cause (e.g. `PortAlreadyInUseException`, missing required environment variables) and renders a clean, actionable diagnostic banner.

    **Internal Mechanism:** `FailureAnalyzers` iterates through registered analyzers in `SpringApplication.handleRunFailure()` before the context terminates.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q19CustomFailureAnalyzerExample.java"
        ```

    **Common Mistake:** Catching generic `Exception` instead of targeting specific root cause exception types, obscuring unexpected failure modes.

    **Production Consideration:** Custom failure analyzers help platform teams enforce self-service troubleshooting across developer teams.

    **Follow-up Questions:**
    - When does `FailureAnalyzer` execute relative to `ApplicationFailedEvent`?
    - How can you test a custom failure analyzer using `ApplicationContextRunner`?

### Q20: What changed between `META-INF/spring.factories` and `AutoConfiguration.imports` in Spring Boot 3?

??? question "Reveal answer"

    **Short Answer:** Spring Boot 3 replaced auto-configuration registration in `META-INF/spring.factories` with dedicated `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` files containing one auto-configuration class per line.

    **Deep Explanation:** Legacy `spring.factories` loaded all keys into a single combined map, causing unnecessary I/O overhead and complex parsing. The dedicated `.imports` file isolates auto-configuration discovery from other SPI registrations and supports Ahead-of-Time (AOT) compilation and GraalVM native image optimization.

    **Internal Mechanism:** `ImportCandidates.load(AutoConfiguration.class, classLoader)` reads the line-delimited `.imports` resource directly.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q20CustomAutoConfigurationImportsExample.java"
        ```

    **Common Mistake:** Migrating libraries from Spring Boot 2.x to 3.x and leaving auto-configuration entries in `spring.factories`, causing them to be silently ignored.

    **Production Consideration:** Third-party starter authors must migrate to `.imports` to support Spring Boot 3.x and GraalVM native images.

    **Follow-up Questions:**
    - What SPI hooks still use `META-INF/spring.factories` in Spring Boot 3?
    - How does AOT processing precompute condition matching at build time?

### Q21: How do you programmatically bind and validate properties using the `Binder` API?

??? question "Reveal answer"

    **Short Answer:** Use `Binder.get(environment)` or construct a `Binder` from `ConfigurationPropertySource` to bind properties into strongly typed objects with custom conversion and validation.

    **Deep Explanation:** The `Binder` API is the underlying engine powering `@ConfigurationProperties`. Programmatic binding is essential in custom infrastructure plugins, dynamic tenant configuration loaders, and custom EnvironmentPostProcessors where annotation-driven injection is not yet available.

    **Internal Mechanism:** `Binder.bind(ConfigurationPropertyName, Bindable, BindHandler)` normalizes property names, applies converters from `ConversionService`, and validates constraints using `ValidationBindHandler`.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q21BinderApiProgrammaticBindingExample.java"
        ```

    **Common Mistake:** Parsing raw string properties from `Environment.getProperty()` and manually converting types, missing relaxed binding and validation.

    **Production Consideration:** Use `BindHandler` callbacks to log bound property sources and audit configuration origins during startup.

    **Follow-up Questions:**
    - How do you register custom converters with the `Binder`?
    - How does `BindHandler.onSuccess` enable property audit logging?
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Scenario Questions

### Q22: Production Incident: Sensitive database credentials leaked via `/actuator/env`. How do you audit, remediate, and harden the system?

??? question "Reveal answer"

    **Short Answer:** Rotate all exposed credentials immediately. Restrict actuator exposure to `health` and `info`, set `management.endpoint.env.show-values=never`, move management endpoints to an internal private port, and require authentication for privileged diagnostic endpoints.

    **Deep Explanation:** Wildcard Actuator exposure (`include: "*"`) without authentication exposes the `/actuator/env` endpoint. If `show-values` is configured to `always`, sanitized property values (passwords, tokens, keys) are revealed in plaintext to unauthenticated HTTP clients.

    **Internal Mechanism:** `EnvironmentEndpoint` queries the `Environment` property sources. Spring Boot applies `SanitizingFunction` to mask values matching sensitive keys (e.g. `*password*`, `*secret*`), but setting `show-values: always` displays them without masking.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q22UnsecuredActuatorIncidentScenarioExample.java"
        ```

    **Common Mistake:** Relying solely on default property sanitization keywords; custom property names like `api-key` or `token-v2` may not match default sanitization patterns.

    **Production Consideration:** Always isolate management traffic on a dedicated port (`management.server.port=9090`) and block that port on external ingress load balancers.

    **Follow-up Questions:**
    - How do you configure a custom `SanitizingFunction` bean to mask proprietary secret formats?
    - How do you configure Spring Security to require `ROLE_ACTUATOR_ADMIN` on management endpoints?

### Q23: Production Incident: Spikes of 502 Bad Gateway errors and dropped background jobs during Kubernetes rolling updates. What is the root cause and fix?

??? question "Reveal answer"

    **Short Answer:** Default `server.shutdown=immediate` closes the HTTP server instantly upon receiving `SIGTERM`, severing active connections, and unmanaged worker thread pools are killed without draining. Fix by configuring `server.shutdown=graceful`, tuning shutdown timeouts, and using `ThreadPoolTaskExecutor` with `setWaitForTasksToCompleteOnShutdown(true)`.

    **Deep Explanation:** During a Kubernetes rolling deployment, pods receive a `SIGTERM`. If graceful shutdown is disabled, in-flight HTTP requests fail immediately before upstream ingress controllers remove the pod IP from endpoints. Similarly, background jobs executing on unmanaged threads are abruptly killed mid-transaction.

    **Internal Mechanism:** `server.shutdown=graceful` triggers `WebServer.shutDownGracefully()`, rejecting new connections with TCP close while allowing in-flight requests to complete within `spring.lifecycle.timeout-per-shutdown-phase`.

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q23SigtermInflightTaskDropScenarioExample.java"
        ```

    **Common Mistake:** Setting a Spring Boot shutdown timeout (e.g. 60s) that is larger than Kubernetes `terminationGracePeriodSeconds` (default 30s), causing Kubernetes to forcefully kill the pod with `SIGKILL` before Spring finishes draining.

    **Production Consideration:** Align container `terminationGracePeriodSeconds` with Spring Boot's `timeout-per-shutdown-phase` and pre-stop sleep hooks to allow ingress routing tables to propagate.

    **Follow-up Questions:**
    - Why is a `preStop` sleep hook often recommended in Kubernetes pod specs alongside graceful shutdown?
    - How does `SmartLifecycle` phase ordering coordinate database pool shutdown after task executor completion?
<!-- --8<-- [end:scenarios] -->
