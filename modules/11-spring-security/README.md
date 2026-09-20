# Module 11 — Spring Security

This module contains theory, runnable review exercises, unit tests, and production-grade implementations covering Spring Security architecture:
- `SecurityFilterChain` ordering, filter proxy chains, and exception translation
- Authentication vs Authorization (`AuthenticationManager`, `ProviderManager`, `SecurityContextHolder`)
- Password hashing mechanics (`DelegatingPasswordEncoder`, BCrypt, Argon2, timing attack protection)
- Insecure Direct Object Reference (IDOR / BOLA) prevention & principal ownership checks
- Method Security (`@EnableMethodSecurity`, `@PreAuthorize`, SpEL expression evaluation)
- Cryptographic JWT signature validation & token lifecycle
- CSRF protection strategies (Stateful cookie sessions vs Stateless bearer tokens) & CORS whitelisting
- Credential sanitization in audit logs and error handlers

## Canonical Documentation
See [`docs/topics/spring-security/`](../../docs/topics/spring-security/index.md) for full theory, question banks, code reviews, and production guides.
