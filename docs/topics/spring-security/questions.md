# Spring Security Interview Questions

Four levels of interview questions covering authentication, authorization, SecurityFilterChain architecture, password hashing, JWT validation, IDOR mitigation, method security, and production incident remediation.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the difference between Authentication and Authorization in Spring Security?

??? question "Reveal answer"
    - **Authentication**: The process of validating **who** a user is. It establishes identity by verifying credentials (passwords, certificates, tokens) and populates the `Authentication` principal in `SecurityContextHolder`.
    - **Authorization**: The process of determining **what** an authenticated identity is permitted to do. It evaluates roles, permissions, scopes, and resource ownership before granting access.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q01AuthenticationVsAuthorization.java"
        ```

### 2. How does the `SecurityFilterChain` architecture work and what is the execution order of core filters?

??? question "Reveal answer"
    Incoming HTTP requests traverse `DelegatingFilterProxy` in the Servlet container, which delegates to `FilterChainProxy` in Spring's `ApplicationContext`. `FilterChainProxy` executes an ordered list of security filters:
    `SecurityContextHolderFilter` $\rightarrow$ `HeaderWriterFilter` $\rightarrow$ `CorsFilter` $\rightarrow$ `CsrfFilter` $\rightarrow$ `LogoutFilter` $\rightarrow$ `UsernamePasswordAuthenticationFilter` / `BearerTokenAuthenticationFilter` $\rightarrow$ `ExceptionTranslationFilter` $\rightarrow$ `AuthorizationFilter`.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q02SecurityFilterChainArchitecture.java"
        ```

### 3. How does `DelegatingPasswordEncoder` work and why is plain MD5 or SHA-256 unsuitable for passwords?

??? question "Reveal answer"
    Standard cryptographic hash functions like MD5 or SHA-256 are designed for fast computation and lack random per-user salts and tunable work factors, making them vulnerable to precomputed rainbow tables and GPU cracking.
    
    `DelegatingPasswordEncoder` prefixes stored hashes with an algorithm identifier (e.g. `{bcrypt}`, `{argon2}`) and applies adaptive, memory/CPU-hard key stretching with automatic random salts and constant-time verification.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q03PasswordEncoderMechanics.java"
        ```

### 4. How does `SecurityContextHolder` store authentication state and when must it be cleared?

??? question "Reveal answer"
    `SecurityContextHolder` stores the `SecurityContext` using a `ThreadLocal` strategy (`MODE_THREADLOCAL`) by default. Because Servlet containers and thread pools reuse worker threads across requests, the context must always be cleared (via `SecurityContextHolder.clearContext()`) at request completion or in a `finally` block to prevent identity leakage across requests.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q04SecurityContextHolderStrategies.java"
        ```

### 5. What is the difference between Roles and Authorities in Spring Security?

??? question "Reveal answer"
    In Spring Security, a **Role** is simply a `GrantedAuthority` that begins with the prefix `ROLE_` (e.g. `ROLE_ADMIN`).
    - `hasRole("ADMIN")` automatically prepends `ROLE_` and looks for authority `ROLE_ADMIN`.
    - `hasAuthority("order:write")` checks for the exact authority string without modifying it.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q05RolesVsAuthorities.java"
        ```

### 6. What are the common HTTP authentication schemes (Basic, Bearer, Session Cookie) and their use cases?

??? question "Reveal answer"
    - **HTTP Basic (`Authorization: Basic base64(user:pass)`)**: Simple, stateless, but transmits credentials on every request; used for internal APIs and service-to-service calls.
    - **Bearer Token (`Authorization: Bearer <jwt>`)**: Cryptographically signed stateless access token used in OAuth2 and modern microservice APIs.
    - **Session Cookie (`Cookie: JSESSIONID=...`)**: Stateful session stored in server memory/Redis; standard for server-rendered web applications and form logins.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q06AuthenticationSchemesOverview.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 7. How does Method Security work in Spring Security 6+ with `@PreAuthorize` and SpEL?

??? question "Reveal answer"
    Enabling `@EnableMethodSecurity` activates AOP method interception via `AuthorizationManagerBeforeMethodInterceptor`. Method annotations like `@PreAuthorize` evaluate Spring Expression Language (SpEL) expressions against the current `Authentication` and method arguments (e.g. `@PreAuthorize("hasRole('ADMIN') or #owner == authentication.name")`) before method execution.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q07MethodSecurityPreAuthorize.java"
        ```

### 8. What is the anatomy of a JWT and what critical validations must be performed?

??? question "Reveal answer"
    A JWT comprises three Base64Url segments: `Header.Payload.Signature`.
    Critical validations:
    1. Cryptographic HMAC or RSA/ECDSA signature verification using the shared secret or public key.
    2. Explicit algorithm check (rejecting `"alg": "none"`).
    3. Expiration verification (`exp > now`).
    4. Issuer (`iss`) and audience (`aud`) verification.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q08JwtStructureAndSignature.java"
        ```

### 9. What is the role of an OAuth2 Resource Server versus Authorization Server?

??? question "Reveal answer"
    - **Authorization Server (IdP)**: Authenticates the resource owner and issues cryptographically signed access tokens and OIDC ID tokens (e.g. Keycloak, Auth0, Okta).
    - **Resource Server**: The backend REST API that validates incoming access tokens (via JWT public key / JWKS or token introspection) and serves protected data.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q09OAuth2ResourceServerConcepts.java"
        ```

### 10. When is CSRF protection necessary and how does the Synchronizer Token Pattern operate?

??? question "Reveal answer"
    CSRF is mandatory for applications using browser session cookies (`JSESSIONID`) where browsers automatically attach cookies to cross-site requests. The Synchronizer Token Pattern generates a unique, unguessable token (`X-XSRF-TOKEN`) stored in a cookie/session and mandates that mutating HTTP requests (`POST`, `PUT`, `DELETE`) submit that token in a custom header. Purely stateless APIs using `Authorization: Bearer` headers do not require CSRF protection.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q10CsrfProtectionMechanisms.java"
        ```

### 11. What are the major pitfalls in Spring Security CORS configuration?

??? question "Reveal answer"
    - Setting `allowedOriginPatterns("*")` paired with `allowCredentials(true)` allows arbitrary malicious domains to read authenticated responses via browser `fetch` (CWE-942).
    - Placing CORS configuration outside Spring Security's `CorsFilter` can cause CORS preflight `OPTIONS` requests to be rejected with `401 Unauthorized` before reaching CORS validation.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q11CorsConfigurationRules.java"
        ```

### 12. What is the difference between `AuthenticationEntryPoint` and `AccessDeniedHandler`?

??? question "Reveal answer"
    Both are handled by `ExceptionTranslationFilter`:
    - **`AuthenticationEntryPoint`**: Invoked when an unauthenticated / anonymous user attempts to access a protected resource, responding with `401 Unauthorized` or redirecting to the login page.
    - **`AccessDeniedHandler`**: Invoked when an authenticated user has insufficient roles or permissions, responding with `403 Forbidden`.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q12AuthenticationEntryPointVsAccessDenied.java"
        ```

### 13. Why should custom authentication filters extend `OncePerRequestFilter`?

??? question "Reveal answer"
    `OncePerRequestFilter` guarantees that a filter's logic executes at most once per request dispatch within a single request lifecycle, preventing duplicate token validation or authentication overhead during servlet error dispatches, async dispatches, or forward operations.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q13CustomOncePerRequestFilter.java"
        ```

### 24. How does Spring Security configure HSTS, Content-Security-Policy (CSP), and X-Frame-Options to mitigate clickjacking and XSS?

??? question "Reveal answer"

    **Short Answer:** Spring Security's `HeaderWriterFilter` attaches default defensive HTTP response headers. `X-Frame-Options: DENY` stops clickjacking by preventing iframe embedding; `X-Content-Type-Options: nosniff` halts MIME-confusion attacks; `Strict-Transport-Security` (HSTS) forces browsers to use HTTPS; and a custom `Content-Security-Policy` (CSP) restricts executable script origins to defeat Cross-Site Scripting (XSS).

    **Internal Mechanism:** `HeaderWriterFilter` executes early in `SecurityFilterChain`. It queries registered `HeaderWriter` beans (e.g. `XFrameOptionsHeaderWriter`, `HstsHeaderWriter`) and writes HTTP header values directly to `HttpServletResponse` before downstream controllers render response bodies.

    **Common Mistake:** Disabling frame options globally with `headers(h -> h.frameOptions(f -> f.disable()))` to make a single third-party widget work, which leaves all authenticated application pages vulnerable to clickjacking.

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q24SecurityHeadersHstsCspXFrame.java"
        ```

### 25. How does Spring Security prevent session fixation attacks and limit concurrent sessions per user?

??? question "Reveal answer"

    **Short Answer:** Spring Security applies `changeSessionId()` on successful authentication to prevent session fixation, migrating existing session attributes to a freshly issued session ID while invalidating the old ID. Concurrency control utilizes `ConcurrentSessionFilter` and `SessionRegistry` to limit maximum concurrent sessions per principal (e.g., maximum 1 active device).

    **Internal Mechanism:** `SessionManagementFilter` checks `SessionAuthenticationStrategy`. Upon login, `ChangeSessionIdAuthenticationStrategy` calls `HttpServletRequest.changeSessionId()`. If concurrent session limits are exceeded, `ConcurrentSessionControlAuthenticationStrategy` either rejects the new login (`maxSessionsPreventsLogin(true)`) or marks the oldest session as expired in `SessionRegistry`.

    **Common Mistake:** Enabling concurrent session control in a clustered multi-instance environment without providing a distributed `SessionRegistry` (e.g. Spring Session with Redis), resulting in per-instance session limits that fail across load-balanced nodes.

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q25SessionFixationAndConcurrencyControl.java"
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 14. How do you safely propagate `SecurityContext` across Virtual Threads and asynchronous executors?

??? question "Reveal answer"
    Standard `ThreadLocal` does not automatically propagate to child threads or Virtual Threads. Using `MODE_INHERITABLETHREADLOCAL` is dangerous with thread pools due to context retention across pooled tasks.
    
    In Java 21+ and Spring Security 6+, wrap async tasks using `DelegatingSecurityContextExecutor` or explicitly capture the `SecurityContext` and set it within the task's execution boundary, ensuring `clearContext()` is called in a `finally` block.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q14SecurityContextVirtualThreads.java"
        ```

### 15. What architectural patterns prevent Insecure Direct Object Reference (IDOR / BOLA)?

??? question "Reveal answer"
    1. **Data-Driven Ownership Queries**: Scope database queries to the authenticated user ID (`WHERE id = :id AND owner_id = :currentUserId`).
    2. **Method Security with SpEL**: Apply `@PreAuthorize("#entity.ownerId == authentication.name or hasRole('ADMIN')")`.
    3. **Opaque Cursors / Tenant Partitioning**: Avoid sequential integer IDs and enforce multi-tenant domain boundaries.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q15IdorBolaPreventionPatterns.java"
        ```

### 16. How is authentication propagated in a Zero Trust Microservices Gateway architecture?

??? question "Reveal answer"
    The API Gateway validates the external public OAuth2 token, verifies tenant status, strips public credentials, and generates an internal cryptographically signed assertion token (e.g. short-lived internal JWT or signed mTLS headers) containing verified user identity and permissions to propagate downstream.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q16ZeroTrustGatewayTokenPropagation.java"
        ```

### 17. How do you implement distributed brute-force protection and rate limiting in Spring Security?

??? question "Reveal answer"
    Use a distributed cache (e.g. Redis) storing IP and username failure counts with TTLs (e.g. sliding window / token bucket). Once failure thresholds are breached, intercept requests before password hashing executes and immediately return `429 Too Many Requests` with a `Retry-After` header.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q17RateLimitingAndBruteForceProtection.java"
        ```

### 18. How does JWKS (JSON Web Key Set) key rotation work in production OAuth2 resource servers?

??? question "Reveal answer"
    The Authorization Server signs JWTs with a private key and publishes active public keys at `/.well-known/jwks.json`, each tagged with a unique Key ID (`kid`). Resource servers cache the JWKS set and resolve the matching public key using the incoming token's `kid` header, supporting seamless key rotation without deployment downtime.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q18JwksKeyRotationMechanics.java"
        ```

### 19. How do Spring Security mechanisms map to the OWASP API Security Top 10?

??? question "Reveal answer"
    - **API1:2023 (BOLA/IDOR)** $\rightarrow$ Principal ownership checks and `@PreAuthorize`.
    - **API2:2023 (Broken Authentication)** $\rightarrow$ DelegatingPasswordEncoder & JWT signature validation.
    - **API3:2023 (Broken Object Property Level Authorization)** $\rightarrow$ DTO record decoupling.
    - **API4:2023 (Unrestricted Resource Consumption)** $\rightarrow$ Rate limiting filters and bounded pagination.
    - **API5:2023 (Broken Function Level Authorization)** $\rightarrow$ Strict `requestMatchers` hierarchy.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q19OwaspTop10ApiSecurityMapping.java"
        ```

### 26. How do you implement fine-grained, dynamic Attribute-Based Access Control (ABAC) using Spring Security 6's `AuthorizationManager`?

??? question "Reveal answer"

    **Short Answer:** Implement `AuthorizationManager<T>` where `T` is the invocation context (e.g. `RequestAuthorizationContext` or domain context). The manager evaluates environmental and domain attributes (tenant ID, IP subnet, resource sensitivity classification) against the authenticated principal and returns an `AuthorizationDecision(boolean granted)`.

    **Deep Explanation:** Traditional Role-Based Access Control (RBAC) relies on static strings (`hasRole('ADMIN')`). In complex multi-tenant enterprise platforms, access depends on context: a manager in Tenant A should not view confidential records in Tenant B, even with identical administrative roles. `AuthorizationManager` replaces legacy `AccessDecisionManager` and `Voter` hierarchies with a unified functional interface capable of inspecting custom domain attributes.

    **Internal Mechanism:** `AuthorizationFilter` delegates HTTP request evaluation to `AuthorizationManager.check(Supplier<Authentication>, T)`. The manager inspects the authentication and context object, returning an `AuthorizationDecision`. If `decision.isGranted()` is false, `AuthorizationFilter` publishes an `AuthorizationDeniedEvent` and throws `AccessDeniedException`.

    **Example:** [Dynamic ABAC implementation](/topics/spring-security/concepts.md).

    **Common Mistake:** Encoding complex business and tenancy rules into brittle SpEL expressions inside `@PreAuthorize`, creating untestable strings that fail only at runtime when syntax errors occur.

    **Production Consideration:** Keep attribute evaluation lightweight. If checking access requires querying external policy stores (like Open Policy Agent / OPA), cache policy decisions with short TTLs (e.g. 5–30s) or pre-load user clearance claims directly into JWT tokens.

    **Follow-up Questions:**
    - How does Spring Security 7 lambda DSL simplify custom `AuthorizationManager` registration in `SecurityFilterChain`? See [Java 25 / Boot 4 Spring Security: Lambda DSL](/tracks/java25-boot4/spring-security/questions.md)
    - How do you isolate tenant data access at the database level? See [Database / SQL: Multi-Tenancy Patterns](/topics/database-sql/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q26DynamicAttributeAuthorizationManager.java"
        ```

### 27. How do you configure a Spring Security OAuth2 Resource Server to dynamically resolve multiple tenant JWKS issuers at runtime?

??? question "Reveal answer"

    **Short Answer:** Configure an `AuthenticationManagerResolver<HttpServletRequest>` or dynamic `JwtIssuerReactiveAuthenticationManagerResolver`. The resolver inspects the unverified JWT `iss` claim or request hostname, validates it against a tenant registry whitelist, and delegates validation to the tenant-specific `JwtDecoder` and JWKS public key endpoint.

    **Deep Explanation:** In multi-tenant B2B architectures, each corporate tenant maintains its own Identity Provider (Okta, Azure AD, Keycloak) with distinct private signing keys and token issuers. A static single-issuer resource server cannot validate tokens from multiple identity providers. Spring Security supports multi-tenant token resolution by deferring `JwtDecoder` creation until the request arrives.

    **Internal Mechanism:** `JwtAuthenticationProvider` is resolved per request. An initial parser inspects the unverified JWT header/claims to extract `iss`. If the issuer is registered, a cached `NimbusJwtDecoder` bound to that tenant's `/.well-known/jwks.json` verifies the cryptographic signature.

    **Example:** [Multi-tenant OAuth2 Resource Server](/topics/spring-security/internals.md).

    **Common Mistake:** Allowing arbitrary unvalidated `iss` URLs to trigger dynamic JWKS retrieval without an allowlist, allowing attackers to supply an external attacker-controlled issuer URL (SSRF attack) and forge valid tokens signed with their own keys.

    **Production Consideration:** Always enforce a strict, immutable tenant whitelist. Cache tenant `JwtDecoder` instances in memory with bounded Caffeine caches so each request does not recreate decoder instances.

    **Follow-up Questions:**
    - How do OAuth2 Resource Servers cache JWKS public keys while handling zero-downtime key rotation? See [Spring Security: JWKS Rotation](#18-how-does-jwks-json-web-key-set-key-rotation-work-in-production-oauth2-resource-servers)
    - How does Zero-Trust gateway token propagation secure internal downstream calls? See [Spring Security: Zero Trust Gateway](#16-how-is-authentication-propagated-in-a-zero-trust-microservices-gateway-architecture)

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q27MultiTenantJwtIssuerRouting.java"
        ```

### 28. How does Spring Security authenticate client certificates via `X509AuthenticationFilter` and validate Subject Alternative Names (SANs)?

??? question "Reveal answer"

    **Short Answer:** `X509AuthenticationFilter` extracts client X.509 certificates from the `javax.servlet.request.X509Certificate` request attribute populated by TLS termination. It extracts the Subject DN (e.g. `CN`) or Subject Alternative Names (DNS/URI SANs) and wraps them in a `PreAuthenticatedAuthenticationToken` for `AuthenticationManager` validation.

    **Deep Explanation:** Mutual TLS (mTLS) authenticates clients at the transport layer before any application HTTP byte is processed. When applied to microservices, services present cryptographic certificates issued by an internal PKI (e.g. HashiCorp Vault or cert-manager). Spring Security integrates with mTLS by mapping client certificate identities to internal service roles and granted authorities.

    **Internal Mechanism:** When mutual TLS handshake succeeds, the servlet container or ingress proxy forwards the client certificate chain in servlet request attributes or `X-Forwarded-Client-Cert` headers. `X509AuthenticationFilter` extracts the principal using `X509PrincipalExtractor` and delegates to `PreAuthenticatedAuthenticationProvider`.

    **Example:** [Pre-authenticated mTLS filter flow](/topics/spring-security/concepts.md).

    **Common Mistake:** Relying on plain Common Name (CN) matching instead of validating the Certificate Authority (CA) chain and Subject Alternative Name (SAN), allowing expired or rogue certificates from unrelated CAs to authenticate.

    **Production Consideration:** When terminating mTLS at an API Gateway (Envoy/Nginx), configure strict mutual TLS between gateway and backend, or ensure the gateway signs forwarded certificate headers with HMAC to prevent header spoofing from untrusted external networks.

    **Follow-up Questions:**
    - How does mTLS prevent Man-In-The-Middle (MITM) attacks in cloud environments? See [Architecture: Zero Trust Network Security](/topics/architecture/questions.md)
    - How do you configure `RestClient` to supply client certificates for outbound mTLS calls? See [REST API: HTTP Clients](/topics/rest-api/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q28MtlsX509CertificateAuthentication.java"
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenario

### 20. Production Incident: Customer invoice data leaked via IDOR vulnerability. How do you diagnose and remediate?

??? question "Reveal answer"
    - **Root Cause**: The invoice endpoint fetched records by UUID path variable without verifying that the requesting user matched the invoice's owner in the database.
    - **Remediation**: Extract the authenticated principal from `SecurityContext`, pass it to the service layer, verify ownership before returning records, and write ArchUnit/contract tests enforcing principal verification across all resource controllers.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q20IdorDataBreachIncident.java"
        ```

### 21. Production Incident: Attacker bypasses authentication by submitting unsigned JWT with `alg: none`. How do you fix it?

??? question "Reveal answer"
    - **Root Cause**: A custom JWT filter decoded the payload Base64 content without verifying signatures or explicitly checking the `alg` header property.
    - **Remediation**: Use a hardened `JwtTokenValidator` or standard Spring Security OAuth2 Resource Server that explicitly rejects `alg: none` and verifies HMAC/RSA signatures in constant-time.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q21JwtAlgNoneSignatureBypassIncident.java"
        ```

### 22. Production Incident: Anonymous users intermittently observe administrative privileges due to `ThreadLocal` context leakage. How do you resolve it?

??? question "Reveal answer"
    - **Root Cause**: An asynchronous task or filter set an administrative authentication context on a pooled thread but failed to invoke `SecurityContextHolder.clearContext()` in a `finally` block when done.
    - **Remediation**: Enforce `SecurityContextHolderFilter` execution and wrap all asynchronous task submissions with `DelegatingSecurityContextRunnable` or `try-finally` cleanup.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q22ThreadLocalContextLeakIncident.java"
        ```

### 23. Production Incident: Cross-origin credentials stolen via wildcard CORS configuration. How do you remediate?

??? question "Reveal answer"
    - **Root Cause**: The application configured `allowedOriginPatterns("*")` paired with `allowCredentials(true)`, allowing any third-party website to make credentialed requests and read user financial balances.
    - **Remediation**: Restrict `allowedOrigins` to an explicit whitelist of trusted frontend domains, enable CSRF protection, and configure `SameSite=Strict` on session cookies.

    ??? example "Example"
        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q23WildcardCorsCredentialTheftIncident.java"
        ```

### 29. Production Incident: Timing attack on HMAC signature verification allows API key forgery

??? question "Reveal answer"

    **Short Answer:** An external API webhook gateway validated incoming HMAC-SHA256 signatures using standard `String.equals()`. By measuring sub-microsecond response latency differences across millions of automated requests, an attacker deduced the expected signature byte-by-byte, forged authentic webhooks, and credited customer accounts without payment.

    **Deep Explanation:** Standard string comparison operators (`String.equals()`) terminate early upon encountering the first non-matching character. If the first character matches, comparison takes slightly longer than if it mismatched. Over thousands of statistical samples, network jitter is averaged out, exposing a timing side-channel that reveals the valid signature one byte at a time.

    **Internal Mechanism:** `String.equals()` performs an element-by-element loop:
    `if (v1[i] != v2[i]) return false;`
    An attacker measuring HTTP response times with microsecond resolution discovers the correct character by observing which candidate byte generates the longest processing latency.

    **Example:** [Constant-time signature verification](/topics/spring-security/code-review.md).

    **Common Mistake:** Using `String.equals()` or `Arrays.equals()` for cryptographic secrets, signatures, and authentication tokens.

    **Production Consideration:** Always use constant-time comparison algorithms like `MessageDigest.isEqual(byte[] a, byte[] b)`. Constant-time comparisons always inspect every byte regardless of mismatch position, completely eliminating timing side-channels.

    **Follow-up Questions:**
    - How does `BCrypt.checkpw` ensure constant-time password verification? See [Spring Security: Password Encoders](#3-how-does-delegatingpasswordencoder-work-and-why-is-plain-md5-or-sha-256-unsuitable-for-passwords)
    - How does JWT signature verification prevent timing attacks? See [Spring Security: JWT Verification](#8-what-is-the-anatomy-of-a-jwt-and-what-critical-validations-must-be-performed)

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q29TimingAttackHmacVerificationIncident.java"
        ```

### 30. Production Incident: OAuth2 authorization code flow triggers infinite HTTP-to-HTTPS redirect loop behind reverse proxy

??? question "Reveal answer"

    **Short Answer:** Following an ingress infrastructure upgrade to AWS ALB with TLS termination, users attempting SSO login experienced browser `ERR_TOO_MANY_REDIRECTS`. The Spring Boot OAuth2 client generated HTTP redirect URIs (`http://app.com/login/oauth2/code/oidc`), causing the ALB to redirect back to HTTPS in an infinite loop.

    **Deep Explanation:** The reverse proxy terminates TLS and forwards requests to Spring Boot over plain HTTP. By default, Spring Boot and Tomcat construct redirect URIs and absolute links using `HttpServletRequest.getScheme()`, which reads `http`. When the application redirects the user to the Identity Provider with `redirect_uri=http://...`, the proxy intercepts it, issues a `301 Moved Permanently` to `https://...`, and restarts the login flow indefinitely.

    **Internal Mechanism:** Spring Boot does not trust `X-Forwarded-*` headers by default to prevent HTTP Host header poisoning from untrusted external networks. Without `server.forward-headers-strategy=FRAMEWORK`, `ForwardedHeaderFilter` is not active, and Tomcat ignores the proxy's `X-Forwarded-Proto: https` header.

    **Example:** [Reverse proxy TLS termination](/topics/spring-security/production.md).

    **Common Mistake:** Hardcoding absolute HTTPS URLs in application code or disabling SSL redirect at the load balancer, exposing plain HTTP traffic.

    **Production Consideration:** Configure `server.forward-headers-strategy=FRAMEWORK` in `application.yml` and restrict trusted proxy CIDR ranges at the edge network so `ForwardedHeaderFilter` safely reconstructs external HTTPS schemes and ports.

    **Follow-up Questions:**
    - How does `OAuth2AuthorizationRequestRedirectFilter` construct authorization request URLs? See [Spring Security: OAuth2 Client](/topics/spring-security/internals.md)
    - How do Kubernetes ingress controllers propagate `X-Forwarded-For` and client IP headers? See [Docker: Container Ingress](/topics/docker/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/11-spring-security/src/examples/java/lab/springsecurity/questions/Q30OAuth2RedirectLoopTlsTerminationIncident.java"
        ```
<!-- --8<-- [end:scenarios] -->
