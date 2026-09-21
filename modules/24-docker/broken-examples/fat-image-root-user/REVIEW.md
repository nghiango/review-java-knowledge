# Code Review — Fat Single-Stage Image Running as Root

## Context

A development team containerized a Spring Boot 3.5 payment service using Docker. An engineer authored `Dockerfile` to build and package the application artifact.

Review `Dockerfile` for image layer bloat, build toolchain leakage, attack surface expansion, and non-root execution boundaries.

## What to look for

- Single-stage build copying build tools (Maven / Gradle) and source code into the production runtime
- Default execution as `root` (UID 0) and privilege escalation risks
- Base image selection (full SDK vs slim/distroless JRE)
- Cache layer invalidation ordering (`COPY . .` before dependency resolution)

## Runnable check

To inspect layer sizes and root execution:

```bash
docker build -t payment-service:review -f Dockerfile .
docker history payment-service:review
docker run --rm payment-service:review whoami # outputs: root
```
