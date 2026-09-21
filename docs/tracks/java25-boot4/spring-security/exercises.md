# Spring Security Hands-On Exercises — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline exercises in [`docs/topics/spring-security/exercises.md`](../../../topics/spring-security/exercises.md) cover implementing custom authentication providers, JWT filters, and method security evaluators.
    These exercises focus on **Spring Boot 4.0 / Spring Security 7 & Java 25 skills**:

---

## Exercise 1: Refactor Legacy Fluent Chaining to Security 7 Lambda DSL

### Objective
Given a legacy configuration using `.and().csrf().disable().and().authorizeRequests()`:
1. Rewrite the configuration using modern `Customizer` lambdas.
2. Replace `authorizeRequests()` with `authorizeHttpRequests(auth -> ...)`.
3. Configure `sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`.
4. Ensure the authorization rules end with `anyRequest().denyAll()` or `authenticated()`.

### Verification Kata
Run unit tests verifying that:
- `/public/**` allows anonymous access.
- `/admin/**` rejects unauthenticated users with 401/403.
- Unmapped paths default to 403 Forbidden.

---

## Exercise 2: Implement Zero-Leak Security Context Propagation with ScopedValue

### Objective
Create a scoped context coordinator for high-concurrency background processing:
1. Define a `ScopedValue<SecurityContext>`.
2. Write a utility method `executeInSecurityContext(SecurityContext ctx, Runnable task)` that binds the scoped value.
3. Spawn 50 virtual threads using `StructuredTaskScope` and verify that each virtual thread reads the correct scoped user context.
4. Verify that once the structured task scope exits, the context is absent and cannot be read by carrier threads.
