# Solution — Secrets Baked into Docker Image

## Annotated code

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Security issue: Passing sensitive credentials via ARG.
# Values passed via --build-arg are permanently recorded in the image manifest and visible to anyone
# who runs `docker history <image>` or inspects the image configuration JSON.
ARG GITHUB_API_TOKEN
ARG DB_PASSWORD

# Security issue: Baking plaintext secrets into ENV instructions.
# Environment variables set via ENV persist across all child image layers and are inspectable
# by any user with read access to the image registry via `docker inspect <image>`.
ENV DB_USER="app_admin"
ENV DB_PASSWORD="${DB_PASSWORD}"
ENV GITHUB_TOKEN="${GITHUB_API_TOKEN}"

COPY app.jar .

# Observability issue: Printing token prefixes or masked values during build logs can leak entropy into CI build logs.
RUN echo "Configuring private access with token: ${GITHUB_TOKEN:0:4}****"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Issue list

### Security issue: Sensitive credentials baked into `ARG` and `ENV` metadata

- **Location:** `Dockerfile:7`
- **Description:** `ARG` and `ENV` instructions persist into the image configuration blob. Even if a subsequent `RUN unset GITHUB_TOKEN` is executed, previous layers retain the secret forever in the image history.
- **Impact:** Anyone with pull permissions to the Docker container registry (developers, third-party contractors, compromised CI workers) can extract plaintext database passwords and API tokens using `docker history --no-trunc <image>`.
- **Remediation:** Never use `ARG` or `ENV` for sensitive credentials. Use Docker BuildKit secret mounts (`RUN --mount=type=secret,id=github_token ...`) during build time, and inject runtime configuration via Kubernetes Secrets, AWS Secrets Manager, or mounted volume files at container runtime.

### Observability issue: Credential logging in build outputs

- **Location:** `Dockerfile:17`
- **Description:** Logging partial token prefixes in `echo` commands.
- **Impact:** CI/CD runners capture stdout/stderr to build logs, exposing tokens in plain text across log aggregation platforms.
- **Remediation:** Remove all logging of credentials and configure secret masking on CI runners.

## Correct implementation

See [`correct/Dockerfile`](correct/Dockerfile).

Detailed discussion in [Solutions](../../../docs/topics/docker/solutions.md#secure-secrets-management-and-buildkit-mounts).
