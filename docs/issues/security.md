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

## Related

- [Issue catalogue](index.md)
