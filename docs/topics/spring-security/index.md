# Spring Security Architecture & Engineering

## Why this matters

Security is non-negotiable in enterprise systems. A single architectural oversight—such as evaluating request matchers in the wrong order, failing to validate JWT cryptographic signatures, trusting client-supplied identifiers without principal cross-checks (IDOR/BOLA), or disabling CSRF on session-cookie authentication—can result in catastrophic data breaches, regulatory penalties, and compliance failures (PCI-DSS, GDPR, SOC 2).

Senior backend engineers must understand the Spring Security architecture inside-out: `SecurityFilterChain` proxy interceptor ordering, `AuthenticationManager` / `ProviderManager` pipelines, `SecurityContext` storage and thread boundaries, password hashing mechanics (`DelegatingPasswordEncoder`, Argon2, BCrypt), method authorization via SpEL, and stateless vs stateful security invariants.

## Core Concepts

- [Authentication vs Authorization, SecurityFilterChain Architecture, Password Hashing, JWT Tokens, OAuth2 Resource Server, CSRF, and CORS](concepts.md)
- [FilterChainProxy, SecurityContextHolderFilter, ProviderManager, ExceptionTranslationFilter, and AuthorizationManager Interceptors](internals.md)

## How it works internally

Understand how Spring Security routes requests through `DelegatingFilterProxy` to `FilterChainProxy`, manages authentication via `ProviderManager` and `UserDetailsService`, stores principals in `SecurityContextHolder`, evaluates method security with `AuthorizationManagerBeforeMethodInterceptor`, and translates exceptions in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions across 4 tiers: 6 Basic, 7 Intermediate, 6 Senior, and 4 Production Scenarios with dedicated runnable code examples.

## Common Production Problems

IDOR data breaches, JWT `alg: none` signature bypasses, `ThreadLocal` context leakage on pooled executor threads, and credential harvesting from unmasked logs are diagnosed in [Production](production.md).

## Broken Examples

1. [Weak password hashing and insecure verification](code-review.md#weak-password-hashing-and-insecure-verification)
2. [Insecure Direct Object Reference (IDOR / BOLA)](code-review.md#insecure-direct-object-reference-idor-bola)
3. [Overly broad permitAll and requestMatcher ordering](code-review.md#overly-broad-permitall-and-requestmatcher-ordering)
4. [Unvalidated JWT signature and expiration](code-review.md#unvalidated-jwt-signature-and-expiration)
5. [Missing method security and parameter-based authorization](code-review.md#missing-method-security-and-parameter-based-authorization)
6. [CORS and CSRF misconfiguration](code-review.md#cors-and-csrf-misconfiguration)
7. [Credential logging and sensitive data exposure](code-review.md#credential-logging-and-sensitive-data-exposure)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

Choosing between stateful HTTP sessions (with CSRF tokens and server-side invalidation) versus stateless JWT bearer tokens (with distributed validation and token revocation challenges); URL-level versus method-level `@PreAuthorize` security; and BCrypt work factors versus CPU latency budgets.

## Production Checklist

- Use `DelegatingPasswordEncoder` with strong adaptive work factors (BCrypt log rounds $\ge 12$ or Argon2id) and constant-time comparison.
- Order `authorizeHttpRequests` strictly from most-specific/highest-privilege to least-specific, ending with `anyRequest().authenticated()`.
- Validate all incoming JWTs cryptographically with HMAC/RSA verification, clock skew tolerance, and strict `exp` checks; explicitly reject `alg: none`.
- Implement data-driven ownership authorization checks on all resource endpoints to eliminate IDOR / BOLA vulnerabilities.
- Enable `@EnableMethodSecurity` and use `@PreAuthorize` for defense-in-depth across domain services.
- Never disable CSRF on cookie-authenticated web applications. Only disable CSRF for purely stateless bearer-token APIs.
- Whitelist CORS origins explicitly; never combine `allowedOriginPatterns("*")` with `allowCredentials(true)`.
- Redact and mask `Authorization` headers, session tokens, and passwords in all logging and error reporting pipelines.

## Senior-Level Questions

Explore advanced topics like SecurityContext propagation across Virtual Threads, Zero Trust API gateway authentication, JWKS key rotation, and distributed rate limiting in [Senior Questions](questions.md#senior).

## Exercises

Hands-on Spring Security katas to practice custom authentication providers, JWT filters, and dynamic SpEL authorization rules in [Exercises](exercises.md).

## Related

- [REST API](../rest-api/index.md)
- [Spring MVC](../spring-mvc/index.md)
- [Spring Core](../spring-core/index.md)
- [Spring Boot](../spring-boot/index.md)
