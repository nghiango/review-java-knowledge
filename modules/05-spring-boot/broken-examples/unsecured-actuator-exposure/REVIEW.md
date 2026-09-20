# Code Review Target: Unsecured Actuator Exposure

Review the following Spring Boot Actuator configuration and application configuration file. Identify security and operational risks associated with endpoint exposure.

## Files Under Review

- `ActuatorSecurityConfig.java`
- `application.yml`

## Review Objectives

1. Determine which actuator endpoints are exposed over HTTP.
2. Evaluate data leakage risks from sensitive diagnostic endpoints (`/actuator/env`, `/actuator/heapdump`).
3. Evaluate availability risks from mutating endpoints (`/actuator/shutdown`).
