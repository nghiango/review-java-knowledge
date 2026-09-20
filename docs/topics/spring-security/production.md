# Spring Security in Production

Production operations, incident diagnosis, cryptographic key management, and security hardening for Spring Security deployments.

## 1. Password Hashing Performance & Work Factor Tuning

BCrypt is intentionally computationally intensive to deter brute force attacks. Selecting an appropriate work factor (cost) balances CPU consumption against cryptographic resilience.

### Empirical Cost Factor Sizing

$$\text{Work Factor } N \implies 2^N \text{ hash iterations}$$

- **Cost 10**: ~50–100 ms on modern server CPU (minimum acceptable for web logins).
- **Cost 12**: ~250–500 ms on modern server CPU (**recommended production baseline**).
- **Cost 14+**: ~1000–2000 ms (reserved for offline batch hashing or ultra-sensitive admin credentials).

```java
@Bean
public PasswordEncoder passwordEncoder() {
    // 12 is recommended for balanced security and latency
    return new BCryptPasswordEncoder(12);
}
```

### Rate Limiting & CPU Denial of Service Mitigation
Because password verification consumes significant CPU, attackers can execute denial of service (DoS) attacks by flooding login endpoints with bogus passwords. Always place rate limiting (e.g. Bucket4j or API Gateway rate limiters) ahead of authentication endpoints.

---

## 2. Diagnosing CORS & CSRF Failures in Single-Page Applications

Cross-origin and CSRF misconfigurations represent common production integration hurdles between modern frontend SPAs (React, Vue, Angular) and Spring Boot backends.

### Incident Symptoms & Remediation

| Symptom | Root Cause | Production Remediation |
|---|---|---|
| Browser console shows `CORS error: No 'Access-Control-Allow-Origin' header` on preflight `OPTIONS` | Preflight `OPTIONS` request blocked by Spring Security filter chain | Permit `OPTIONS` or configure `CorsConfigurationSource` on `HttpSecurity.cors()` |
| Browser blocks response with `credentials flag is 'true' but 'Access-Control-Allow-Origin' is '*'` | Wildcard origin used with cookies/credentials | Explicitly specify allowed origins: `.setAllowedOrigins(List.of("https://app.example.com"))` |
| `403 Forbidden` on `POST`/`PUT`/`DELETE` for session-based web app | CSRF token missing or mismatch in request header | Configure SPA to send `X-XSRF-TOKEN` header extracted from `XSRF-TOKEN` cookie |
| Intermittent `Invalid CSRF Token` after session expiration | Client caching stale CSRF cookie across session boundaries | Implement 401 response interceptor in SPA to redirect to login on session invalidation |

---

## 3. JWT Lifecycle & Production Token Hardening

When using stateless JWT authentication in microservices architectures:

### 1. Cryptographic Key Management & JWKS
- Never hardcode HMAC shared secrets in source code or properties files.
- Prefer asymmetric signatures (RS256 or ES256) where authentication services sign tokens using a private key and downstream resource servers verify tokens using public keys retrieved from a JSON Web Key Set (JWKS) endpoint (`/.well-known/jwks.json`).
- Cache JWKS public keys with an in-memory TTL (e.g. 1 hour) with automatic fallback refresh on unknown `kid` (Key ID) headers to support zero-downtime key rotation.

### 2. Short Lifespans and Refresh Tokens
- **Access Tokens**: Short-lived (5–15 minutes). Minimizes the window of exposure if a token is intercepted.
- **Refresh Tokens**: Longer-lived (7–30 days), stored in secure `HttpOnly`, `SameSite=Strict` cookies or dedicated backend session stores, rotatable upon each exchange.

### 3. Clock Skew Handling
- Distributed servers can experience clock drift. Configure a bounded clock skew allowance (typically 30–60 seconds) during `exp` and `nbf` validation to prevent spurious rejections.

---

## 4. Production Spring Security Hardening Checklist

- [ ] All passwords hashed using `BCryptPasswordEncoder(12)` or `Argon2PasswordEncoder`.
- [ ] No `Authorization` headers, session IDs, or raw passwords logged in application logs or APM traces.
- [ ] Security filter chain ordering puts specific restrictive paths (`/api/admin/**`) ahead of broad wildcard paths.
- [ ] Terminal rule in `authorizeHttpRequests` is explicitly `.anyRequest().authenticated()` or `.anyRequest().denyAll()`.
- [ ] Resource access endpoints validate ownership against authenticated `Principal` (IDOR defense).
- [ ] Defense-in-depth method security (`@EnableMethodSecurity`, `@PreAuthorize`) enabled on business services.
- [ ] CORS origins explicitly whitelisted; never use wildcard `*` with credentials enabled.
- [ ] CSRF enabled for stateful cookie-based session architectures; disabled only for purely stateless Bearer-token APIs.
- [ ] HTTP Security Headers enabled (HSTS, Content-Security-Policy, X-Frame-Options: DENY, X-Content-Type-Options: nosniff).
- [ ] `SecurityContextHolder.setStrategyName()` evaluated for virtual threads (`MODE_INHERITABLETHREADLOCAL` vs scoped values).
