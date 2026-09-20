# Spring Security Solutions

Production-grade implementations corresponding to the code review exercises.

## Safe password encoder and registration

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/passwords/UserAccount.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/passwords/SafePasswordEncoderConfig.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/passwords/UserRegistrationService.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/passwords/SafeRegistrationController.java"
```

### Why it works

1. **Adaptive BCrypt Hashing**: `BCryptPasswordEncoder(12)` uses an adaptive cost factor (work factor $2^{12} = 4096$ iterations) with a cryptographically secure, per-user unique salt embedded in the MCF string.
2. **Timing-Attack Resistance**: `passwordEncoder.matches(raw, encoded)` uses constant-time comparison algorithms that do not terminate early on character mismatches, thwarting timing analysis.
3. **DelegatingPasswordEncoder Upgrade Path**: `PasswordEncoderFactories.createDelegatingPasswordEncoder()` prefixes hashes with an algorithm identifier (e.g. `{bcrypt}`, `{argon2}`), enabling seamless password hash upgrades as hardware speeds increase.
4. **Bean Validation**: Input parameters enforce strict non-null, length, and complexity constraints before any processing.

### Trade-offs

BCrypt consumes bounded CPU cycles per authentication attempt to deter brute force attacks. For high-throughput services, rate limiting and load balancing must be sized according to password hashing concurrency.

---

## Safe invoice controller with ownership validation

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/idor/Invoice.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/idor/InvoiceService.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/idor/SafeInvoiceController.java"
```

### Why it works

1. **Principal Binding**: The requesting user is resolved directly from Spring Security's authenticated `Principal` or `@AuthenticationPrincipal`, never from untrusted path/query parameters.
2. **Ownership Verification**: `InvoiceService.getInvoiceForUser` queries the repository with both the invoice ID and owner identifier (`findByInvoiceIdAndOwnerId`), preventing horizontal privilege escalation.
3. **Role-Based Privilege Elevation**: Users with administrative authority (`ROLE_ADMIN`) are allowed cross-tenant read access through dedicated administrative endpoints or explicit role checks.

### Trade-offs

Requires database queries to include tenant/owner predicates, requiring composite indexing on `(id, owner_id)` for optimal query performance.

---

## Safe security filter chain configuration

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/filterchain/SafeSecurityFilterChainConfig.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/filterchain/AdminDashboardController.java"
```

### Why it works

1. **Strict Most-Specific-First Order**: Most restrictive paths (`/api/admin/**`) are declared before broader patterns (`/api/public/**` and `/api/**`).
2. **Explicit Deny-by-Default**: Terminal rules specify `.anyRequest().authenticated()` or `.anyRequest().denyAll()`, eliminating inadvertent access leaks on newly added endpoints.
3. **Zero Wildcard Leaks**: Public whitelists are scoped strictly to specific public resources without capturing nested administrative routes.

### Trade-offs

Requires strict developer discipline during routing updates. Security filter chains must be covered by integration tests that verify authorization rejection on sensitive subpaths.

---

## Cryptographic JWT token validation

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/jwt/JwtClaims.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/jwt/JwtTokenValidator.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/jwt/SafeJwtAuthenticationFilter.java"
```

### Why it works

1. **HMAC-SHA256 Digital Signature Verification**: `Mac.getInstance("HmacSHA256")` computes and verifies signatures using `MessageDigest.isEqual()` constant-time comparison before inspecting any claims.
2. **Algorithm Enforcement**: Explicitly requires `"alg": "HS256"`, completely rejecting `alg: none` and algorithm-switching exploits.
3. **Expiration & Not-Before Validation**: Validates `exp` timestamp against current epoch seconds (with configurable clock skew), rejecting expired or prematurely used tokens.
4. **Stateless Authentication Population**: Successfully verified claims populate `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`, clearing contexts on validation failure.

### Trade-offs

Stateless JWT validation cannot be revoked instantly without maintaining a distributed token blacklist (e.g. in Redis) or using short token lifespans paired with refresh tokens.

---

## Method security with PreAuthorize

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/methodsecurity/Document.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/methodsecurity/MethodSecurityConfig.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/methodsecurity/DocumentService.java"
```

### Why it works

1. **`@EnableMethodSecurity`**: Activates Spring Security's AOP method interception across the service layer.
2. **SpEL Expression Authorization**: `@PreAuthorize("hasRole('ADMIN') or #document.owner == authentication.name")` validates caller identity and domain object ownership at invocation time.
3. **Defense-in-Depth**: Method security protects business operations whether invoked from REST controllers, GraphQL resolvers, scheduled background jobs, or message queues.

### Trade-offs

Method security introduces reflection and SpEL evaluation overhead (~microseconds per call) and relies on Spring proxy interception (requires public methods and external bean invocation).

---

## Safe session and stateless CSRF CORS configuration

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/csrf/SafeSessionSecurityConfig.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/csrf/SafeStatelessSecurityConfig.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/csrf/SafeFundsTransferController.java"
```

### Why it works

1. **Cookie-Based Applications**: For session-based web apps, CSRF protection is enabled with `CookieCsrfTokenRepository.withHttpOnlyFalse()` or `HttpSessionCsrfTokenRepository`, requiring clients to supply the CSRF token in an `X-XSRF-TOKEN` request header for mutating verbs.
2. **Stateless REST APIs**: Pure stateless APIs using `SessionCreationPolicy.STATELESS` and Bearer JWTs safely disable CSRF (`csrf.disable()`) because browsers do not automatically attach Bearer headers on cross-site navigation.
3. **Strict Origin Whitelisting**: CORS explicitly configures exact trusted origins (e.g. `https://trusted-bank.example.com`) and specific HTTP methods, completely avoiding wildcard `allowedOriginPatterns("*")` when `allowCredentials(true)` is enabled.

### Trade-offs

Session-based frontends (SPAs) must be configured to read CSRF cookies and set the matching request header on all mutating HTTP calls (`POST`, `PUT`, `DELETE`).

---

## Security audit logging and credential sanitization

### Implementation

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/sanitization/SecurityAuditFilter.java"
```

```java
--8<-- "modules/11-spring-security/src/main/java/lab/springsecurity/sanitization/SanitizedAuthController.java"
```

### Why it works

1. **Header Redaction**: `SecurityAuditFilter` intercepts request/response logging, masking sensitive headers (`Authorization`, `Proxy-Authorization`, `Cookie`, `Set-Cookie`) with `[REDACTED]`.
2. **No Credential Logging**: Authentication endpoints log usernames, client IPs, and status codes, but never raw passwords or tokens.
3. **Generic Error Responses**: Authentication failures return standardized error messages without echoing user-supplied passwords or leaking account existence information.

### Trade-offs

Redacting headers requires filter-level processing on incoming requests, adding minor overhead to audit log serialization.
