# Spring Security Tests

## Security Testing Strategy

Testing Spring Security architectures requires verifying both positive authorization flows and negative access-denied enforcement:

1. **Password Encoding & Salt Verification**: Ensures passwords hashed with BCrypt produce valid Modular Crypt Format strings with distinct salts and cannot be matched against plaintexts via weak comparison algorithms.
2. **Horizontal Privilege Enforcement (IDOR)**: Verifies that authenticated requests for resources belonging to other principals are rejected with `403 Forbidden` or `404 Not Found`.
3. **Filter Chain & Route Ordering**: Validates that most-specific admin routes enforce `hasRole('ADMIN')` while public endpoints remain accessible.
4. **JWT Signature & Expiration Checks**: Confirms that forged signatures, expired tokens, and `alg: none` tokens are rejected during filter execution.
5. **Method Security & SpEL Evaluation**: Tests that `@PreAuthorize` guards service methods against unauthorized callers even when invoked programmatically.
6. **CSRF & CORS Boundary Enforcement**: Validates that session-based mutating endpoints reject requests missing CSRF tokens, and CORS rejects untrusted origins.
7. **Audit Redaction & Credential Protection**: Ensures raw credentials and `Authorization` headers are masked in audit trails and error responses.

## Test Suite Overview

```bash
./gradlew :modules:11-spring-security:test
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `PasswordEncoderTest` | BCrypt generates unique salts per hash and performs constant-time matching | Unit test with BCryptPasswordEncoder |
| `SafeInvoiceControllerTest` | Authenticated users can only read their own invoices; admin can read any invoice | Spring MVC MockMvc / Unit assertions |
| `SecurityFilterChainTest` | `/api/admin/**` requires `ROLE_ADMIN`; `/api/public/**` is accessible without authentication | Filter chain request matching assertions |
| `JwtTokenValidatorTest` | Cryptographic HMAC signature validation rejects tampered tokens, expired tokens, and `alg: none` | HMAC-SHA256 signature verification tests |
| `DocumentMethodSecurityTest` | `@PreAuthorize` allows document owner or admin, rejecting unauthorized users | SecurityContextHolder + service invocation |
| `CsrfCorsSecurityTest` | Session mutating requests require CSRF token; CORS rejects unlisted origins | MockMvc / header validation |
| `SecurityAuditFilterTest` | `Authorization` headers and sensitive parameters are redacted in audit logs | Servlet filter invocation & assertion |

## Key Test Snippets

### Proving BCrypt Work Factor and Constant-Time Password Verification

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/passwords/PasswordEncoderTest.java"
```

### Proving Insecure Direct Object Reference (IDOR) Prevention

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/idor/SafeInvoiceControllerTest.java"
```

### Proving Top-Down Security Filter Chain Ordering

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/filterchain/SecurityFilterChainTest.java"
```

### Proving JWT Signature Integrity and Expiration Verification

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/jwt/JwtTokenValidatorTest.java"
```

### Proving Defense-in-Depth Method Security

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/methodsecurity/DocumentMethodSecurityTest.java"
```

### Proving CSRF and CORS Boundary Enforcement

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/csrf/CsrfCorsSecurityTest.java"
```

### Proving Sensitive Header Sanitization in Audit Trails

```java
--8<-- "modules/11-spring-security/src/test/java/lab/springsecurity/sanitization/SecurityAuditFilterTest.java"
```

## Related

- [Solutions](solutions.md)
- [Code Review](code-review.md)
- [Production](production.md)
