# Spring Security Interview Questions — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline questions in [`docs/topics/spring-security/questions.md`](../../../topics/spring-security/questions.md) cover authentication vs authorization, password hashing, JWT basics, and CSRF protection.
    These questions focus on the **Spring Boot 4.0 / Spring Security 7 & Java 25 delta**: lambda-only configuration DSL, virtual thread security context propagation with ScopedValue, removal of `and()`, and migration patterns.

---

## Basic Questions

### 1. How does the lambda-only DSL in Spring Security 7 replace legacy method chaining and improve configuration safety?

??? question "Reveal answer"
    In Spring Security 7, all `HttpSecurity` configuration methods strictly mandate a `Customizer` lambda parameter (e.g. `authorizeHttpRequests(auth -> auth...)`). Legacy parameterless methods and fluent `.and()` calls are completely removed. This guarantees that each subsystem (authorization, CSRF, session management, CORS) is configured within its own self-contained lexical scope, eliminating ambiguous builder chaining and configuration leakage.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q01LambdaOnlyDslBasicsExample.java"

### 2. Why was the and() method chaining removed in Spring Security 7, and how does nested lambda scoping prevent configuration bugs?

??? question "Reveal answer"
    The `.and()` method returned an outer builder reference, but in complex nested security configurations, developers often lost track of which configurer `.and()` was actually returning to. This caused misconfigurations where subsequent rules applied to the wrong filter or were silently ignored. Nested lambdas enforce compiler-checked boundaries, ensuring configuration parameters cannot escape their intended scope.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q02RemovalOfAndChainingExample.java"

### 3. How does SecurityContextHolder interact with virtual threads in Spring Boot 4, and what is the default strategy?

??? question "Reveal answer"
    By default, `SecurityContextHolder` uses `ThreadLocalSecurityContextHolderStrategy`. When running on Java 25 virtual threads, thread-local storage is bound to the `VirtualThread` instance itself, not the underlying carrier thread. When a virtual thread unmounts during blocking I/O and remounts onto a different carrier thread, its thread-local security context travels seamlessly with it, ensuring thread isolation without carrier leakage.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q03SecurityContextHolderVirtualThreadsExample.java"

### 4. What are the differences between authorizeHttpRequests and the obsolete authorizeRequests in Spring Security?

??? question "Reveal answer"
    `authorizeRequests()` is an obsolete API from Spring Security 5 based on `AccessDecisionManager` and metadata attributes (`ConfigAttribute`). In contrast, `authorizeHttpRequests()` uses the modern, high-performance `AuthorizationManager` API, integrates with `RequestMatcherDelegatingAuthorizationManager`, supports pure lambda configuration, and resolves authorization decisions before invoking downstream filters.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q04AuthorizeHttpRequestsVsAuthorizeRequestsExample.java"

---

## Intermediate Questions

### 5. Why is SecurityContextHolder.MODE_INHERITABLETHREADLOCAL hazardous when applications run on virtual threads?

??? question "Reveal answer"
    `MODE_INHERITABLETHREADLOCAL` copies thread-local variables from parent threads to newly created child threads. In a virtual thread environment where an application may spawn millions of short-lived virtual threads, copying thread-local maps creates massive heap allocation churn and memory retention. Furthermore, if tasks are submitted to pooled executor workers, the inherited context is never refreshed, causing context bleed across unrelated tasks.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q05InheritableThreadLocalHazardVirtualThreadsExample.java"

### 6. How do Java 25 Scoped Values solve security context propagation across concurrent tasks with zero memory leaks?

??? question "Reveal answer"
    Java 25 `ScopedValue` provides immutable, strictly scoped values bound to a lexical execution frame (`ScopedValue.where(KEY, value).run(...)`). Child virtual threads or subtasks spawned within structured task scopes automatically share the bound value without map copying or memory retention. When the lexical block exits, the binding is immediately out of scope, guaranteeing zero context leakage with zero cleanup boilerplate.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q06ScopedValueSecurityContextPropagationExample.java"

### 7. How has Spring Security modernized method security annotations and SpEL evaluations in modern Spring applications?

??? question "Reveal answer"
    Spring Security 7 uses `AuthorizationManager`-based method interceptors for `@PreAuthorize`, `@PostAuthorize`, `@Secured`, and JSR-250 annotations. SpEL expressions (e.g. `hasRole('ADMIN') or #customerId == authentication.name`) are compiled and evaluated against a streamlined `MethodSecurityEvaluationContext`, providing better performance and native integration with Java records and JSpecify nullness annotations.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q07MethodSecurityPreAuthorizeSpelExample.java"

### 8. How are CSRF and CORS configured cleanly in Spring Security 7 using modern Customizer functions?

??? question "Reveal answer"
    Spring Security 7 configures CSRF and CORS via pure lambda customizers. For stateless REST microservices, CSRF is disabled via `csrf(AbstractHttpConfigurer::disable)`. For browser-facing cookie-based APIs, CSRF repository customizers configure `CookieCsrfTokenRepository.withHttpOnlyFalse()`. CORS is configured using `cors(cors -> cors.configurationSource(...))`, eliminating legacy method chaining.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q08ModernCsrfCorsCustomizersExample.java"

---

## Senior Questions

### 9. How do you refactor a legacy Spring Security configuration using and() chaining into the modern Spring Security 7 lambda DSL?

??? question "Reveal answer"
    Refactoring requires breaking apart fluent chains connected by `.and()` into discrete lambda blocks passed to configurer methods:
    1. Replace `http.authorizeRequests()` with `http.authorizeHttpRequests(auth -> auth...)`.
    2. Replace `.and().csrf().disable()` with `.csrf(AbstractHttpConfigurer::disable)`.
    3. Replace `.and().sessionManagement().sessionCreationPolicy(...)` with `.sessionManagement(session -> session.sessionCreationPolicy(...))`.
    4. Replace `.and().httpBasic()` with `.httpBasic(Customizer.withDefaults())`.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q09MigrationSecurity6To7DslRefactorExample.java"

### 10. How do you migrate legacy InheritableThreadLocal security context propagation to Java 25 Scoped Values?

??? question "Reveal answer"
    Migrating involves:
    1. Removing `SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)`.
    2. Defining a static final `ScopedValue<SecurityContext> SECURITY_CONTEXT = ScopedValue.newInstance()`.
    3. Wrapping asynchronous or virtual thread task execution in `ScopedValue.where(SECURITY_CONTEXT, currentContext).run(task)` or utilizing Spring's contextual task executors.
    4. Accessing context via `SECURITY_CONTEXT.isBound() ? SECURITY_CONTEXT.get() : fallback`.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q10MigrationInheritableThreadLocalToScopedValueExample.java"

### 11. How do you perform high-concurrency stateless JWT authentication on virtual threads without thread pinning?

??? question "Reveal answer"
    Stateless JWT verification involves CPU-bound cryptographic signature verification (HMAC, RSA, ECDSA) and claim parsing. Under virtual threads, ensure that JWT parsing and public key fetching (JWKS) do not execute inside blocking `synchronized` blocks that could pin carrier threads, and cache public keys in-memory (e.g. using Caffeine) with bounded background refresh to avoid blocking remote HTTP calls on every incoming request.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q11VirtualThreadJwtAuthenticationExample.java"

### 12. How do RoleHierarchy and modern AuthorizationManager collaborate to provide transitive role-based authorization?

??? question "Reveal answer"
    `RoleHierarchy` defines role inheritance rules (e.g. `ROLE_ADMIN > ROLE_MANAGER > ROLE_USER`). In Spring Security 7, the `RoleHierarchyImpl` is injected into `RoleHierarchyAuthoritiesMapper` or directly configured in `AuthorizationManager`. When evaluating `hasRole("USER")`, the authorization manager automatically grants access to users possessing `ROLE_ADMIN`, eliminating repetitive multi-role declarations across endpoint matchers.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q12RoleHierarchyAuthorizationManagerExample.java"

---

## Scenario Questions

### 13. Scenario: An async worker thread executed an audit mutation with admin rights left over from a previous task. How does ScopedValue prevent identity spoofing?

??? question "Reveal answer"
    In thread pool and virtual thread worker environments, failing to clear `ThreadLocal` or relying on `InheritableThreadLocal` causes uncleared credentials to linger on reusable threads. An unauthenticated background task running subsequently on that worker will observe the previous admin's context, leading to privilege escalation and falsified audit trails. Java 25 `ScopedValue` strictly bounds context lifetime to the lexical execution frame, making it mathematically impossible for identity to linger after task completion.
    
    ??? example "Example"
        --8<-- "tracks/java25-boot4/modules/11-spring-security/src/examples/java/lab/java25boot4/springsecurity/questions/Q13ScenarioInheritableThreadLocalPrivilegeLeakExample.java"
