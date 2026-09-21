# Solution — Fat Single-Stage Image Running as Root

## Annotated code

```dockerfile
# Performance issue: Using full JDK image (eclipse-temurin:21-jdk) instead of headless JRE or distroless.
# Full JDK includes compiler (javac), header files, and dev tools unnecessary for production runtime (~500MB extra).
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Performance issue: Copying entire repository before build invalidates Docker layer cache on any file touch.
# Source edits bust cache for Gradle dependency downloads, forcing re-download of all dependencies on every commit.
# Security issue: Copying entire workspace (.git, local config, developer secrets) into final production image.
COPY . .

# Performance issue: Single-stage build leaves Gradle caches (~500MB), source code, and intermediate classes in the image layer.
RUN ./gradlew build -x test

EXPOSE 8080

# Security issue: Container process runs as root (UID 0) by default without USER instruction.
# In the event of an arbitrary code execution vulnerability (e.g. Log4Shell / RCE), an attacker gains root access
# within the container and can attempt kernel privilege escalation or mount escaping to compromise the host node.
ENTRYPOINT ["java", "-jar", "build/libs/payment-service-1.0.0.jar"]
```

## Issue list

### Security issue: Container runs as default root user (UID 0)

- **Location:** `Dockerfile:15`
- **Description:** No non-root `USER` is declared, leaving the container entrypoint running under UID 0.
- **Impact:** If an attacker executes remote code via application vulnerabilities, they obtain root within the container namespace, allowing them to install system packages, access host sockets, or exploit kernel vulnerabilities to escape onto the host.
- **Remediation:** Create a dedicated unprivileged user/group (`appuser:appgroup` or UID 10001) and add `USER appuser`.

### Performance issue: Single-stage build creates bloated fat image leaking build tools and caches

- **Location:** `Dockerfile:1`
- **Description:** A single-stage `FROM eclipse-temurin:21-jdk` executes the Gradle build and runs the resulting artifact in the same image filesystem.
- **Impact:** The resulting image exceeds 1.2 GB, containing compiler binaries (`javac`), package managers, Gradle wrapper caches, and source code. This bloats container registry storage, dramatically increases CI/CD deployment push/pull times, and expands the CVE vulnerability footprint.
- **Remediation:** Implement a multi-stage Dockerfile: build the artifact in an ephemeral `builder` stage, extract the Spring Boot layered JAR using `layertools`, and copy only extracted layers into a minimal JRE/distroless base image.

### Performance issue: Cache busting due to premature `COPY . .`

- **Location:** `Dockerfile:6`
- **Description:** Copying all files prior to dependency download invalidates the Docker build cache on every code change.
- **Impact:** Docker cannot cache the dependency download layer; every commit triggers a slow full Gradle re-download.
- **Remediation:** Copy `settings.gradle.kts`, `build.gradle.kts`, and `gradle/` first, resolve dependencies, and then copy source code.

## Correct implementation

See [`correct/Dockerfile`](correct/Dockerfile).

Detailed discussion in [Solutions](../../../docs/topics/docker/solutions.md#multi-stage-builds-layered-jars-and-non-root-execution).
