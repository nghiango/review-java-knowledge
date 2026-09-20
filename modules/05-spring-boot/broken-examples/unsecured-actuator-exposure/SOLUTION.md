# Solution: Unsecured Actuator Exposure

## Annotated Code

```yaml
management:
  endpoints:
    web:
      exposure:
        # Security issue: Exposing wildcard '*' includes dangerous endpoints (env, heapdump, shutdown) over unauthenticated HTTP
        include: "*"
  endpoint:
    health:
      # Security issue: Unconditional 'always' exposes internal database hostnames, disk paths, and component topologies to unauthenticated callers
      show-details: always
    env:
      # Security issue: Showing property values leaks database credentials, API secrets, and encryption keys in plaintext
      show-values: always
    shutdown:
      # Reliability issue: Mutating endpoint allows unauthenticated remote callers to terminate the JVM process
      enabled: true
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `application.yml:5` | `Security` | Wildcard endpoint exposure (`include: "*"`) | Exposes sensitive operational and diagnostic endpoints without authentication. |
| `application.yml:8` | `Security` | Detailed health disclosure (`show-details: always`) | Leaks infrastructure details, database connection strings, and disk storage metadata to public probes. |
| `application.yml:10` | `Security` | Plaintext environment variable display (`show-values: always`) | Sanitization is bypassed, leaking API keys, database credentials, and internal environment configurations. |
| `application.yml:12` | `Reliability` | Enabled shutdown endpoint (`shutdown.enabled: true`) | Allows unauthenticated denial-of-service by triggering graceful or immediate application termination remotely. |

## Correct implementation

- Package: `lab.springboot.actuatorsecurity`
- Production reference: `ActuatorSecurityConfig.java`, `HealthConfig.java`
- Fix: Expose only minimal, safe endpoints (`health`, `info`, `metrics`), restrict `show-details` to `when_authorized` or health groups (`readiness`, `liveness`), disable remote `shutdown`, and place management endpoints on a dedicated internal port (`management.server.port`).
