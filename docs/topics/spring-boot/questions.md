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

### Q24: How does relaxed binding and JSR-380 validation work in `@ConfigurationProperties`?

??? question "Reveal answer"

    **Short Answer:** Relaxed binding matches properties regardless of casing convention (kebab-case, camelCase, snake_case, UPPER_CASE), while `@Validated` triggers JSR-380 Bean Validation at startup to fail-fast on malformed configurations.

    **Internal Mechanism:** The `ConfigurationPropertyName` engine canonicalizes property names into lowercase, dashed words, stripping special characters so environment variables (e.g. `ACME_PAYMENTSERVICE_MAXRETRYATTEMPTS`) bind seamlessly to Java fields (`maxRetryAttempts`).

    **Common Mistake:** Omitting `@Validated` on `@ConfigurationProperties` classes, allowing invalid or missing configuration values to start successfully and fail unexpectedly during production request processing. [Concepts](/topics/spring-boot/concepts.md#4-type-safe-configuration-properties)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q24RelaxedBindingAndValidationExample.java"
        ```

### Q25: How do `@AutoConfigureBefore` and `@AutoConfigureAfter` determine auto-configuration ordering?

??? question "Reveal answer"

    **Short Answer:** In Spring Boot 3, `@AutoConfiguration(before = ..., after = ...)` establishes explicit topological sort order among auto-configuration classes, guaranteeing that prerequisite beans are registered before dependent `@ConditionalOnMissingBean` checks evaluate.

    **Internal Mechanism:** `AutoConfigurationSorter` parses `@AutoConfiguration`, `@AutoConfigureBefore`, and `@AutoConfigureAfter` annotations to build an adjacency graph and computes execution order, ensuring upstream infrastructure beans are registered before downstream starter configurations.

    **Common Mistake:** Using `@Order` or `@Priority` on auto-configuration classes; standard Spring bean ordering annotations have no effect on auto-configuration sorting. [Concepts](/topics/spring-boot/concepts.md#2-auto-configuration-engine)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q25AutoConfigOrderingRulesExample.java"
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

### Q26: What are the startup phases from `ApplicationStartingEvent` to `ApplicationReadyEvent`, and what are the production risks of lazy initialization?

??? question "Reveal answer"

    **Short Answer:** Spring Boot fires events across distinct lifecycle phases: starting environment preparation, context creation, bean instantiation, command-line runners, and readiness probe activation. Lazy initialization accelerates startup but conceals configuration bugs and introduces runtime latency spikes.

    **Deep Explanation:** Setting `spring.main.lazy-initialization=true` prevents beans from instantiating until first accessed. While this reduces microservice cold-start times in local development, production workloads suffer severe first-request tail latency (warmup penalty) and runtime `BeanCreationException`s that would otherwise fail-fast during deployment.

    **Internal Mechanism:** `SpringApplicationRunListeners` broadcasts events: `ApplicationStartingEvent` $\to$ `ApplicationEnvironmentPreparedEvent` $\to$ `ApplicationContextInitializedEvent` $\to$ `ApplicationPreparedEvent` $\to$ `ApplicationStartedEvent` $\to$ `ApplicationReadyEvent`.

    **Example:** [Application startup lifecycle](/topics/spring-boot/concepts.md#6-embedded-runtimes-graceful-shutdown).

    **Common Mistake:** Enabling lazy initialization in production without pre-warming HTTP connection pools, ORM entity graphs, and JIT compiler caches.

    **Production Consideration:** Keep lazy initialization disabled in production environments; use AppCDS and Spring AOT instead to reduce startup duration without sacrificing deployment-time validation.

    **Follow-up Questions:**
    - How does Micrometer track startup milestones using `StartupTimeline` metrics? See [Observability: Startup Telemetry](/topics/observability/questions.md)
    - How does Spring TestContext Framework cache application contexts across integration tests? See [Testing: Context Caching](/topics/testing/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q26StartupPhasesAndLazyInitExample.java"
        ```

### Q27: How do you harden Spring Boot Actuator with custom `HealthIndicator`s and `SanitizingFunction`?

??? question "Reveal answer"

    **Short Answer:** Isolate management endpoints on an internal management port, expose only `health` and `info` publicly, register custom composite `HealthIndicator`s for downstream dependencies, and use `SanitizingFunction` to mask secrets.

    **Deep Explanation:** Exposing Actuator endpoints publicly without network perimeter controls or role-based authentication risks sensitive data exfiltration (via `/actuator/env`) or remote denial-of-service (via `/actuator/heapdump`). Implementing `SanitizingFunction` guarantees custom secret keys (e.g. `api-key`, `private-token`) are redacted from `/actuator/env` and `/actuator/configprops`.

    **Internal Mechanism:** Spring Boot iterates through registered `SanitizingFunction` beans in `EndpointMediaTypes` and sanitizes property pairs before rendering JSON response trees.

    **Example:** [Actuator hardening](/topics/spring-boot/concepts.md#5-actuator-production-telemetry).

    **Common Mistake:** Relying solely on default regex keys (`*password*`, `*secret*`), leaving proprietary credentials unmasked.

    **Production Consideration:** Separate management traffic using `management.server.port=9090`, configure Kubernetes readiness/liveness health groups (`management.endpoint.health.group.readiness.include=...`), and require `ROLE_ACTUATOR_ADMIN` on management endpoints.

    **Follow-up Questions:**
    - How does Spring Security configure authorization rules for Actuator request matchers? See [Spring Security: Actuator Authorization](/topics/spring-security/questions.md)
    - How do Kubernetes readiness probes differ from liveness probes during traffic routing? See [Observability: Health Probes](/topics/observability/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q27ActuatorHardeningAndHealthExample.java"
        ```

### Q28: How does `spring-context-indexer` reduce classpath scanning overhead in large microservice deployments?

??? question "Reveal answer"

    **Short Answer:** `spring-context-indexer` processes annotations at compile time and writes `META-INF/spring.components`. At startup, Spring loads this static candidate list directly, eliminating expensive runtime bytecode scanning across large JAR libraries.

    **Deep Explanation:** In large microservices or multi-module monoliths with hundreds of dependencies, standard `@ComponentScan` uses ASM `ClassReader` to inspect every `.class` file on the classpath during bootstrap. This consumes significant CPU and I/O cycles, inflating container startup latency.

    **Internal Mechanism:** When `CandidateComponentsIndexLoader.loadIndex()` discovers `META-INF/spring.components`, `ClassPathScanningCandidateComponentProvider` switches from filesystem directory traversal to direct candidate index lookup.

    **Example:** [Context indexing optimization](/topics/spring-boot/concepts.md#1-opinionated-defaults-starters).

    **Common Mistake:** Enabling classpath scanning with broad wildcard base packages (`com.*`), which bypasses index optimization and scans every dependency.

    **Production Consideration:** Include `org.springframework:spring-context-indexer` as an annotation processor in all internal modules to optimize container launch times in Kubernetes.

    **Follow-up Questions:**
    - How does modular monolith architecture enforce explicit package sharing boundaries? See [Architecture: Modular Monoliths](/topics/architecture/questions.md)
    - How does Spring Core discover and merge `BeanDefinition` metadata? See [Spring Core: Bean Discovery](/topics/spring-core/questions.md#21-how-does-defaultlistablebeanfactory-register-and-merge-rootbeandefinition-instances)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q28ContextIndexingOptimizationExample.java"
        ```

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

### Q29: Production Incident: Container rolling deployment drops in-flight HTTP requests despite Kubernetes graceful termination

??? question "Reveal answer"

    **Short Answer:** Although `server.shutdown=graceful` was set, the Kubernetes pod lifecycle sent `SIGTERM` simultaneously with endpoint deregistration; upstream ingress routers forwarded requests to the terminating pod before iptables rules propagated. Fix with a container `preStop` sleep hook.

    **Deep Explanation:** In Kubernetes, when a pod enters `Terminating` state, two parallel asynchronous actions occur: (1) kubelet sends `SIGTERM` to the container process, and (2) endpoints controller removes the pod IP from the Service endpoint list and kube-proxy updates iptables/ipvs rules. Because network rule propagation takes several seconds, ingress routers continue routing incoming traffic to the pod after Spring Boot has stopped accepting new connections.

    **Internal Mechanism:** The embedded web server immediately rejects new SYN packets with TCP RST while draining in-flight requests, causing client-facing 502 Bad Gateway errors.

    **Example:** [Kubernetes graceful shutdown](/topics/spring-boot/code-review.md).

    **Common Mistake:** Relying solely on `server.shutdown=graceful` without a `preStop: exec: command: ["sleep", "10"]` hook in the container spec.

    **Production Consideration:** Set `terminationGracePeriodSeconds: 45` in Kubernetes, add a `sleep 10` preStop hook, and configure `spring.lifecycle.timeout-per-shutdown-phase: 30s` to ensure clean connection draining without premature SIGKILL.

    **Follow-up Questions:**
    - How do Docker container lifecycle signals (SIGTERM vs SIGKILL) translate to JVM shutdown hooks? See [Docker: Container Lifecycle](/topics/docker/questions.md)
    - How does Spring MVC handle client connection resets and abort signals? See [Spring MVC: Request Processing](/topics/spring-mvc/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q29KubernetesGracefulShutdownScenarioExample.java"
        ```

### Q30: Production Incident: Dynamic configuration refresh with `@RefreshScope` produces stale state and memory leaks in asynchronous tasks

??? question "Reveal answer"

    **Short Answer:** Asynchronous tasks or long-lived background threads cached direct target instance references instead of calling through the `@RefreshScope` proxy, reading obsolete configuration values and retaining garbage-collected bean instances.

    **Deep Explanation:** Beans marked with `@RefreshScope` are lazily instantiated proxies backed by a target source cache in `RefreshScope`. When `/actuator/refresh` triggers, `ContextRefresher` clears the cache so subsequent method calls instantiate fresh beans with new environment properties. If an async task or thread-pool worker stores the inner target instance in a field or `ThreadLocal`, it bypasses the proxy and holds obsolete configuration permanently.

    **Internal Mechanism:** `RefreshScope.refreshAll()` broadcasts `RefreshScopeRefreshedEvent` and destroys cached bean instances in the scope's internal cache map.

    **Example:** [Dynamic refresh scope](/topics/spring-boot/code-review.md).

    **Common Mistake:** Injecting `@RefreshScope` beans into non-refreshable singleton beans and copying their configuration fields into local state during startup.

    **Production Consideration:** Invoke methods on the proxy directly or re-read properties dynamically from `@ConfigurationProperties` beans without storing local copies; audit memory with heap dumps to detect uncollected legacy scope instances.

    **Follow-up Questions:**
    - How does Spring Cloud Config server distribute configuration change notifications via Spring Cloud Bus? See [Spring Cloud: Centralized Configuration](/topics/spring-cloud/questions.md)
    - How does volatile memory visibility guarantee thread-safe dynamic configuration updates? See [Concurrency: Safe Publication](/topics/concurrency/questions.md#21-what-constitutes-safe-publication-of-shared-objects-in-the-java-memory-model)

    ??? example "Example"

        ```java
        --8<-- "modules/05-spring-boot/src/examples/java/lab/springboot/questions/Q30RefreshScopeDynamicConfigScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->
