# Security Issues

Authentication, authorization, injection, secret handling and unsafe input boundaries.

## Entries

### Stale authenticated user on reused thread

**Type:** Security issue · **Severity:** High · **Difficulty:** Senior

ThreadLocal security or request context that is not cleared can make anonymous work observe the
previous authenticated user on a reused executor thread. Scope the context lexically and remove it
at the boundary.

### Unsecured Actuator Endpoint Exposure

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

Wildcard exposure (`include: "*"`) without authentication exposes `/actuator/env`, `/actuator/heapdump`,
and `/actuator/shutdown`, leaking database credentials, API secrets, and allowing remote denial of service.
Restrict exposed endpoints to safe diagnostics (`health`, `info`, `metrics`), isolate management ports,
and enforce authentication on privileged endpoints.

**Appears in:** `modules/05-spring-boot/broken-examples/unsecured-actuator-exposure`

### Committed Production Secrets in Configuration Profile

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

Hardcoding production API keys, encryption secrets, or database master passwords in profile files
(`application-prod.yml`) permanently commits credentials to version control history. Externalize secrets
using runtime environment variables with placeholder defaults and fail-fast `@NotBlank` validation.

**Appears in:** `modules/05-spring-boot/broken-examples/profile-secrets-in-repo`

### Missing Input Validation on Request Payload

**Type:** Security issue · **Severity:** High · **Difficulty:** Basic

Unvalidated controller endpoints accept blank or malformed data into business processing and persistence layers. Enforce declarative validation via Jakarta annotations (`@NotBlank`, `@Email`, `@Size`, `@Min`) and `@Valid` on controller arguments.

**Appears in:** `modules/06-spring-mvc/broken-examples/missing-input-validation`

### Internal Exception Details Leaked in HTTP Response

**Type:** Security issue · **Severity:** High · **Difficulty:** Intermediate

Exposing raw database error messages and stack traces in HTTP responses reveals database schemas, driver implementations, and internal class names to attackers. Map exceptions via `@RestControllerAdvice` to RFC 9457 `ProblemDetail` with sanitized user-facing descriptions.

**Appears in:** `modules/06-spring-mvc/broken-examples/leaking-stack-traces`

### Wildcard CORS with Credentials Allowed

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

Configuring wildcard origins (`*`) paired with credentials (`allowCredentials: true`) exposes authenticated user sessions to cross-origin extraction by malicious websites. Enforce explicit trusted origin whitelisting.

**Appears in:** `modules/06-spring-mvc/broken-examples/cors-wildcard-credentials`

### Mass Assignment & Sensitive Field Leakage

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Senior

Binding incoming JSON request bodies directly to JPA entities (CWE-915) allows untrusted callers to inject administrative fields (e.g. `role: "ADMIN"`, `isVerified: true`). Directly serializing entities also leaks password hashes and internal metadata. Decouple API contracts using dedicated Java record DTOs.

**Appears in:** `modules/10-rest-api/broken-examples/entity-leakage-and-mass-assignment`

### Weak Password Hashing & Non-Constant-Time Comparison

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Basic

Storing passwords using broken/unsalted hash algorithms (MD5) without adaptive work factors allows rainbow table cracking (CWE-328, CWE-916). Comparing secret hashes using `String.equals()` creates timing side-channels (CWE-208). Use `BCryptPasswordEncoder` with constant-time matching.

**Appears in:** `modules/11-spring-security/broken-examples/plaintext-password-storage`

### Insecure Direct Object Reference (IDOR / BOLA)

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

Exposing private resources through user-supplied identifier parameters without verifying ownership against the authenticated `Principal` allows horizontal privilege escalation across tenants (CWE-639, OWASP API1:2023). Enforce ownership validation in repository queries.

**Appears in:** `modules/11-spring-security/broken-examples/idor-unauthorized-access`

### Overly Broad permitAll & RequestMatcher Ordering Bypass

**Type:** Security issue · **Severity:** High · **Difficulty:** Intermediate

Declaring broad wildcard `permitAll()` rules before specific administrative paths in `authorizeHttpRequests` evaluates rules sequentially (first match wins), inadvertently exposing privileged endpoints without authentication (CWE-285). Order rules from most-specific to least-specific.

**Appears in:** `modules/11-spring-security/broken-examples/overly-broad-permitall`

### Unvalidated JWT Digital Signature & Expiration

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Senior

Parsing JWT payloads without cryptographic HMAC/RSA signature verification allows attackers to forge tokens with arbitrary claims and administrative privileges (CWE-347, OWASP API2:2023). Validate cryptographic signatures, algorithms, and expiration timestamps before trusting claims.

**Appears in:** `modules/11-spring-security/broken-examples/unvalidated-jwt-signature`

### Missing Method Security & Identity Spoofing

**Type:** Security issue · **Severity:** High · **Difficulty:** Senior

Relying solely on URL filter security leaves service methods unprotected when invoked internally or via background jobs. Accepting caller usernames as unverified method parameters enables identity spoofing (CWE-285, CWE-290). Enforce defense-in-depth method security via `@EnableMethodSecurity` and `@PreAuthorize`.

**Appears in:** `modules/11-spring-security/broken-examples/missing-method-security`

### Disabling CSRF on Stateful Cookie Sessions

**Type:** Security issue · **Severity:** High · **Difficulty:** Intermediate

Disabling CSRF protection on applications using session cookies allows malicious third-party websites to execute unauthorized state-changing operations on behalf of authenticated victims (CWE-352). Enable CSRF tokens for cookie-based stateful sessions.

**Appears in:** `modules/11-spring-security/broken-examples/cors-and-csrf-misconfiguration`

### Credential Logging & Sensitive Data Exposure

**Type:** Security issue · **Severity:** High · **Difficulty:** Basic

Writing raw `Authorization` headers, passwords, or session tokens to server logs or echoing passwords back in error responses commits credentials to log stores and client logs (CWE-532, CWE-209). Sanitize and redact sensitive headers and payload fields before logging.

**Appears in:** `modules/11-spring-security/broken-examples/credential-logging-and-exposure`, `modules/22-observability/broken-examples/logging-secrets-pii`

---

### Unmasked Credit Card PAN and CVV Logged in Violation of PCI-DSS

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Logging, PCI-DSS Compliance · **Interview frequency:** High · **Production impact:** Critical

Writing raw 16-digit primary account numbers (PAN) and card verification values (CVV) into application debug logs directly violates PCI-DSS Requirement 3.2. Log data is frequently indexed in unencrypted development or centralized logging stores, exposing customer payment instruments to breach. Never log CVV under any circumstances and mask card numbers so only the last four digits are visible (`****-****-****-1234`).

**Appears in:** `modules/22-observability/broken-examples/logging-secrets-pii`

---

### Root Container Execution

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Docker, Linux User Namespaces, Container Security · **Interview frequency:** High · **Production impact:** Critical

Omitting a `USER` instruction runs container processes as UID 0 (root). If an attacker achieves Remote Code Execution (RCE) or exploits a container breakout vulnerability (e.g., runc CVEs, misconfigured mount bindings, sensitive host path access), they immediately possess root privileges on the underlying host node. Always create a dedicated unprivileged user (`useradd -u 10001 -r -g appuser appuser`) and set `USER 10001:10001` before launching the runtime process.

**Appears in:** `modules/24-docker/broken-examples/fat-image-root-user`

---

### Secrets Baked into Image Layers

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Dockerfile, BuildKit, Secret Management · **Interview frequency:** High · **Production impact:** Critical

Using `ARG` or `ENV` to pass private credentials (such as GitHub PAT tokens, NPM tokens, AWS access keys, or production database passwords) during image build bakes the secret into intermediate image layers. Because Docker images are immutable layer stacks, deleting the file in a subsequent `RUN rm` layer or relying on image flattening does not remove the secret—it remains retrievable by running `docker history --no-trunc` or inspecting tar archives. Use BuildKit secret mounts (`RUN --mount=type=secret,id=token ...`) or inject secrets strictly at container runtime via environment variables or secret volumes.

**Appears in:** `modules/24-docker/broken-examples/secrets-baked-into-image`

---

### Over-Permissive Wildcard IAM Policy

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** AWS IAM, Principle of Least Privilege, Cloud Security · **Interview frequency:** High · **Production impact:** Critical

Granting wildcard actions (`"Action": "*"`, `"s3:*"`, `"kms:*"`) and wildcard resources (`"Resource": "*"`) to application runtime IAM roles empowers container workloads with destructive administrative permissions across the AWS account. In the event of a container compromise via Remote Code Execution (RCE) or a dependency CVE, an attacker can delete databases, purge SQS queues, destroy KMS encryption keys, or exfiltrate company data across buckets. Always scope IAM permissions strictly to required API actions, specific resource ARNs, and condition keys (`kms:EncryptionContext`).

**Appears in:** `modules/25-aws/broken-examples/over-permissive-iam-policy`

---

### Plaintext Secrets in Container Environment Configuration

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** AWS ECS, Secrets Manager, Cloud Security · **Interview frequency:** High · **Production impact:** Critical

Hardcoding production database passwords, live payment gateway tokens, and cryptographic signing keys inside container environment variable blocks (such as ECS task definition `environment` arrays or Docker Compose files) exposes credentials in plaintext to anyone with task inspection permissions. Furthermore, ECS task definition revisions are immutable, leaving permanent plaintext credentials in AWS account audit history. Externalize secrets to AWS Secrets Manager or SSM Parameter Store encrypted under KMS Customer Managed Keys, and inject them at container launch using task execution role permissions.

**Appears in:** `modules/25-aws/broken-examples/secrets-in-environment-variables`

---

### Publicly Accessible Production Database Instance

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Amazon RDS, VPC Security, Network Isolation · **Interview frequency:** High · **Production impact:** Critical

Setting `publicly_accessible = true` on production database instances assigns a public IPv4 address, directly exposing relational database ports (such as PostgreSQL port 5432) to the public internet. Internet-facing databases are vulnerable to automated credential brute-forcing, distributed denial of service, and zero-day exploits in database networking stacks. Production databases must strictly reside within private isolated subnets accessible exclusively from backend application security groups.

**Appears in:** `modules/25-aws/broken-examples/single-az-rds-no-backup`

---

### Suppression of Container Vulnerability Scanner Failures in CI Pipeline

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** CI/CD, Container Security, Trivy, Supply Chain Security · **Interview frequency:** High · **Production impact:** Critical

Configuring container vulnerability scanners (such as Trivy, Grype, or Snyk) with `continue-on-error: true` in CI/CD pipelines suppresses scanner exit codes, allowing vulnerable container images with known `CRITICAL` or `HIGH` Common Vulnerabilities and Exposures (CVEs) to be published and deployed to production. This directly violates SOC2 and PCI-DSS Requirement 6.3.2 compliance mandates. Vulnerability scanners must be configured with blocking exit codes (`--exit-code 1`) for unpatched high/critical vulnerabilities.

**Appears in:** `modules/26-ci-cd/broken-examples/tests-and-security-skipped-on-main`

---

### Decorator Ordering Bug Bypassing Security and Authorization Checks

**Type:** Security issue · **Severity:** Critical · **Difficulty:** Intermediate

**Technology:** Decorator Pattern, Spring Security, Pipeline Composition · **Interview frequency:** High · **Production impact:** Critical

Composing caching or logging decorators outside security or authorization decorators in a processing pipeline allows cached responses to bypass access-control checks. When an authorized administrator queries sensitive data, the outer caching decorator stores the payload keyed only by resource ID. Subsequent requests by unauthenticated or unauthorized users hit the cache and receive the sensitive resource directly without executing the inner authorization decorator. Security decorators must always form the outermost layer of any processing chain or decorator pipeline.

**Appears in:** `modules/29-design-patterns/broken-examples/decorator-order-bug`

## Related

- [Issue catalogue](index.md)
