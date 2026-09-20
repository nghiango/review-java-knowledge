# Spring Boot: Internals & Architecture

Understanding the internal mechanisms of `SpringApplication`, auto-configuration filtering, conditional evaluation, and the `Binder` subsystem.

---

## 1. `SpringApplication.run()` Execution Flow

When `SpringApplication.run(Application.class, args)` is invoked, it coordinates the following sequence:

```mermaid
sequenceDiagram
    autonumber
    participant Main as Application.main()
    participant App as SpringApplication
    participant Listeners as SpringApplicationRunListeners
    participant Env as ConfigurableEnvironment
    participant Ctx as ConfigurableApplicationContext
    participant BF as DefaultListableBeanFactory

    Main->>App: run(args)
    App->>App: createBootstrapContext()
    App->>Listeners: starting(bootstrapContext)
    App->>App: prepareEnvironment(listeners, args)
    App->>Listeners: environmentPrepared(env)
    App->>App: printBanner(env)
    App->>App: createApplicationContext()
    App->>App: prepareContext(ctx, env, listeners)
    App->>Listeners: contextPrepared(ctx)
    App->>Listeners: contextLoaded(ctx)
    App->>Ctx: refresh()
    Note over Ctx,BF: Standard Spring Lifecycle + AutoConfigurationImportSelector
    App->>Listeners: started(ctx)
    App->>App: callRunners(ctx, args)
    App->>Listeners: ready(ctx)
    App-->>Main: return ApplicationContext
```

### Key Phases:
1. **Bootstrap Context**: Initializes `DefaultBootstrapContext` to manage early startup singletons.
2. **Environment Preparation**: Loads `PropertySourceLoader` instances (YAML, properties) and activates profiles.
3. **ApplicationContext Creation**: Chooses between `AnnotationConfigServletWebServerApplicationContext` (Servlet), `AnnotationConfigReactiveWebServerApplicationContext` (Reactive), or `AnnotationConfigApplicationContext` (Non-web).
4. **Context Refresh**: Invokes standard Spring bean factory post-processors and triggers auto-configuration.
5. **Runners Execution**: Executes `ApplicationRunner` and `CommandLineRunner` beans before firing `ApplicationReadyEvent`.

---

## 2. AutoConfigurationImportSelector & Filtering

Auto-configuration is orchestrated via `AutoConfigurationImportSelector`, which implements `DeferredImportSelector`:

```mermaid
flowchart TD
    Imports[Read AutoConfiguration.imports] --> ExcludeFilter[Apply spring.autoconfigure.exclude]
    ExcludeFilter --> AutoConfigGroup[DeferredImportSelector.Group]
    AutoConfigGroup --> ConditionFilter[FilteringSpringBootCondition]
    ConditionFilter --> OnClassFilter[OnClassCondition Check Classpath]
    ConditionFilter --> OnWebFilter[OnWebApplicationCondition]
    OnClassFilter --> AutoConfigSort[AutoConfigurationSorter: @AutoConfigureBefore / After]
    AutoConfigSort --> BeanDefReg[Register Matching Configurations into BeanFactory]
```

### Two-Phase Condition Evaluation:
1. **Class-Level Filtering**: Before class loading or parsing, `OnClassCondition` inspects bytecode with ASM (`MetadataReader`) to discard entire auto-configurations without loading unresolvable classes.
2. **Bean-Level Condition Evaluation**: During bean definition registration, `ConditionEvaluator` assesses `@ConditionalOnMissingBean` and `@ConditionalOnProperty`.

---

## 3. The `Binder` Subsystem

`@ConfigurationProperties` is powered by the Spring Boot `Binder` API (`org.springframework.boot.context.properties.bind.Binder`):

```mermaid
flowchart LR
    Sources[ConfigurationPropertySources: YAML, Env, System] --> Norm[Property Key Normalizer kebab-case]
    Norm --> BindTarget[Bindable Target Type record or POJO]
    BindTarget --> Conversion[ConversionService / Custom Converters]
    Conversion --> Valid[Validator: Bean Validation @Validated]
    Valid --> Instantiated[Bound Immutable Property Object]
```

- **Relaxed Binding**: Normalizes property keys into uniform lowercase alphanumeric tokens so `app.billing-rate`, `app.billingRate`, `app.billing_rate`, and `APP_BILLINGRATE` map to the identical property target.
- **Constructor Binding**: Inspects the canonical constructor of records or classes annotated with `@ConstructorBinding` to instantiate immutable property objects directly.

---

## 4. Graceful Shutdown & Lifecycle Management

When a container orchestrator (e.g. Kubernetes) issues a `SIGTERM`:

1. `GracefulShutdown` phase on the embedded web server (e.g. `TomcatWebServer`) stops accepting new TCP connections.
2. The server allows currently active HTTP requests to complete within `spring.lifecycle.timeout-per-shutdown-phase` (default 30 seconds).
3. The `ApplicationContext` closes, triggering `SmartLifecycle.stop()` on asynchronous task executors and message listeners.
4. Database connection pools (HikariCP) and cache connections (Redis) close after worker threads complete their in-flight tasks.
