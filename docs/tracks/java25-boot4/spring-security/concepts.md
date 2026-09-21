# Spring Security 7 Concepts

!!! info "Delta from baseline"
    Baseline module [`modules/11-spring-security`](../../../topics/spring-security/concepts.md) covers security architecture: authentication filters, authentication managers, user details, and method security.
    This page covers the **Spring Boot 4.0 / Spring Security 7 & Java 25 concepts**: lambda-only configuration DSL, `ScopedValue` context propagation, and modern authorization managers.

---

## 1. The Pure Lambda DSL

In Spring Security 5 and 6, configuration methods supported both fluent chaining via `.and()` and the newer lambda DSL. This created ambiguity and subtle scoping bugs where developers assumed `.and()` restored the root `HttpSecurity` builder, but instead returned to an intermediate configurer.

In Spring Security 7:
- **Zero Fluent Chaining**: Methods like `and()` and parameterless configuration calls (`authorizeRequests()`, `csrf()`, `sessionManagement()`) are completely removed.
- **`Customizer<T>` Pattern**: Every configuration extension accepts a functional interface `Customizer<T>`, allowing developers to use method references (`Customizer.withDefaults()`, `AbstractHttpConfigurer::disable`) or concise lambda blocks.
- **Lexical Scoping**: Each configuration domain (`authorizeHttpRequests`, `sessionManagement`, `csrf`, `cors`) is contained within its own lexical lambda scope, eliminating accidental property contamination.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/public/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .build();
}
```

---

## 2. Virtual Threads and SecurityContext Propagation

Traditionally, background thread context propagation in Spring Security relied on `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)`.

### Why `InheritableThreadLocal` is Hazardous on Virtual Threads
1. **Memory Retention**: Virtual threads are intended to be ephemeral and instantiated in millions. Copying `InheritableThreadLocal` maps to every child thread creates significant memory overhead and prevents unreferenced contexts from being garbage collected.
2. **Carrier Thread & Pool Bleeding**: When tasks are dispatched to thread pools or virtual threads are unmounted/remounted across carrier threads, uncleared `InheritableThreadLocal` entries bleed across unrelated tasks, creating severe privilege escalation vulnerabilities.

### Modern Solution: Java 25 `ScopedValue`
Java 25 `ScopedValue` allows immutable, strictly scoped bindings:
- Context is bound to the current lexical execution frame.
- Child virtual threads automatically read the parent's `ScopedValue` without copying overhead.
- When execution exits the scope, the context is immediately and unconditionally reclaimed with zero cleanup code.

---

## 3. Modern Authorization Managers

Spring Security 7 standardizes on `AuthorizationManager<T>` across all servlet and reactive endpoints:
- Evaluates authorization decisions with `check(Supplier<Authentication> authentication, T object)`.
- Replaces legacy `AccessDecisionManager`, `AccessDecisionVoter`, and `ConfigAttribute`.
- Integrates cleanly with declarative route matchers and role hierarchies.
