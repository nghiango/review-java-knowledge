# Spring Security Code Review

Review each clean source before expanding its answer.

## Weak password hashing and insecure verification

A user registration and authentication controller manages user credential storage and login validation.

```java
--8<-- "modules/11-spring-security/broken-examples/plaintext-password-storage/UserAccount.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/plaintext-password-storage/InsecureRegistrationController.java"
```

Consider cryptographic weaknesses, unsalted digests, lack of adaptive work factor, timing attack risks, and missing input validation.

??? warning "Reveal issues"
    **Security issue — Broken / unsalted MD5 hash algorithm:** Storing passwords using broken cryptographic hash (MD5) without unique salt or adaptive work factor (CWE-328 / CWE-916) allows instant reverse lookup via rainbow tables.

    **Security issue — Non-constant-time comparison:** Comparing secret hashes using `String.equals()` terminates on the first mismatched byte, creating timing side-channels that leak hash bytes (CWE-208).

    **Security issue — Missing input validation:** Endpoints accept null or short passwords without length constraints (CWE-521).

    **Architecture issue — Ad-hoc controller authentication:** Handcrafting user lookup and password comparison in controllers bypasses Spring Security's `PasswordEncoder` and `AuthenticationManager`.

[Correct implementation](solutions.md#safe-password-encoder-and-registration)

---

## Insecure Direct Object Reference (IDOR / BOLA)

A billing microservice exposes customer invoice details via REST endpoints.

```java
--8<-- "modules/11-spring-security/broken-examples/idor-unauthorized-access/Invoice.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/idor-unauthorized-access/InvoiceController.java"
```

Consider Broken Object Level Authorization (BOLA / IDOR) vulnerabilities and lack of ownership validation against the authenticated principal.

??? warning "Reveal issues"
    **Security issue — Insecure Direct Object Reference (IDOR / BOLA, OWASP API1:2023):** The controller exposes private financial records directly through user-supplied identifier parameters (`UUID id`) without verifying that the requesting user owns the resource or has administrative roles (CWE-639).

    **Security issue — Horizontal privilege escalation:** Any authenticated user can access arbitrary invoices by simply guessing or enumerating invoice UUIDs.

[Correct implementation](solutions.md#safe-invoice-controller-with-ownership-validation)

---

## Overly broad permitAll and requestMatcher ordering

A security configuration configures authorization rules for public API endpoints and restricted administrative reporting endpoints.

```java
--8<-- "modules/11-spring-security/broken-examples/overly-broad-permitall/InsecureSecurityConfig.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/overly-broad-permitall/AdminMetricsController.java"
```

Consider authorization bypasses caused by `requestMatchers` evaluation order and overly broad wildcard patterns.

??? warning "Reveal issues"
    **Security issue — RequestMatcher top-down ordering bypass:** Spring Security evaluates `authorizeHttpRequests` sequentially (first match wins). Placing `/api/**` as `permitAll()` before `/api/admin/**` matches all admin requests first, completely bypassing role checks (CWE-285).

    **Security issue — Overly permissive wildcard rules:** Broad wildcards in `permitAll()` inadvertently expose administrative and internal management endpoints.

[Correct implementation](solutions.md#safe-security-filter-chain-configuration)

---

## Unvalidated JWT signature and expiration

A stateless REST API uses a custom `OncePerRequestFilter` to authenticate incoming requests via JSON Web Tokens (JWT).

```java
--8<-- "modules/11-spring-security/broken-examples/unvalidated-jwt-signature/JwtClaims.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/unvalidated-jwt-signature/JwtTokenParser.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/unvalidated-jwt-signature/InsecureJwtFilter.java"
```

Consider critical cryptographic vulnerabilities, lack of signature validation, `alg: none` susceptibility, and missing token expiration checks.

??? warning "Reveal issues"
    **Security issue — Unverified JWT digital signature:** Decoding Base64 payload without verifying the HMAC/RSA signature allows attackers to forge tokens with arbitrary subjects, authorities, and permissions (CWE-347 / OWASP API2:2023).

    **Security issue — Missing token expiration (exp) validation:** Failing to check expiration allows expired or revoked tokens to be replayed indefinitely (CWE-613).

    **Security issue — Vulnerability to alg: none attacks:** Custom parsers that ignore header algorithm fields accept unsigned tokens created by setting `"alg": "none"`.

[Correct implementation](solutions.md#cryptographic-jwt-token-validation)

---

## Missing method security and parameter-based authorization

A document collaboration service handles sensitive corporate reports across users and administrators.

```java
--8<-- "modules/11-spring-security/broken-examples/missing-method-security/DocumentTransferService.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/missing-method-security/InsecureDocumentController.java"
```

Consider lack of defense-in-depth method authorization, parameter spoofing risks, and missing `@PreAuthorize` annotations.

??? warning "Reveal issues"
    **Security issue — Missing method security authorization:** Relying exclusively on URL-level filter security leaves backend service methods vulnerable when invoked by internal beans, message listeners, or scheduled jobs (CWE-285).

    **Security issue — Caller-supplied identity spoofing:** Accepting the requester's username as an untrusted parameter (`String username`) allows attackers to impersonate document owners without credentials (CWE-290).

[Correct implementation](solutions.md#method-security-with-preauthorize)

---

## CORS and CSRF misconfiguration

A banking web application supports cookie-based form login and balance transfers between user accounts.

```java
--8<-- "modules/11-spring-security/broken-examples/cors-and-csrf-misconfiguration/InsecureWebSecurityConfig.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/cors-and-csrf-misconfiguration/AccountFundsController.java"
```

Consider Cross-Site Request Forgery (CSRF) vulnerabilities, improper CSRF disabling on session-based endpoints, and dangerous wildcard CORS credentials configurations.

??? warning "Reveal issues"
    **Security issue — Disabling CSRF on stateful cookie sessions:** Disabling CSRF on applications using session cookies (`JSESSIONID`) allows malicious third-party websites to trigger authenticated mutating actions (fund transfers) on behalf of logged-in victims (CWE-352).

    **Security issue — Wildcard CORS origins with credentials enabled:** Configuring `allowedOriginPatterns("*")` alongside `allowCredentials(true)` allows any domain in the world to execute credentialed requests and read user response bodies (CWE-942).

[Correct implementation](solutions.md#safe-session-and-stateless-csrf-cors-configuration)

---

## Credential logging and sensitive data exposure

A centralized security logging filter and authentication controller log incoming HTTP headers and authentication attempts.

```java
--8<-- "modules/11-spring-security/broken-examples/credential-logging-and-exposure/SecurityLoggingFilter.java"
```

```java
--8<-- "modules/11-spring-security/broken-examples/credential-logging-and-exposure/AuthenticationController.java"
```

Consider credential leakage, raw token exposure in application logs, and password leakage in HTTP error responses.

??? warning "Reveal issues"
    **Security issue — Authorization header and token exposure in logs:** Logging raw `Authorization: Bearer <token>` or `Basic <base64>` headers commits sensitive session credentials to server logs and SIEM aggregators (CWE-532).

    **Security issue — Plaintext password logging:** Writing passwords to application logger streams violates compliance standards (PCI-DSS §3.4, GDPR, SOC 2).

    **Security issue — Password reflection in error responses:** Echoing supplied passwords back in error messages exposes credentials across proxy caches and client-side loggers (CWE-209).

[Correct implementation](solutions.md#security-audit-logging-and-credential-sanitization)
