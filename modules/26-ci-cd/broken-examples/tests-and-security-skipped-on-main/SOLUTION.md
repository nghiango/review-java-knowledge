# Solution: CI & Container Security Pipeline

## Annotated Target

```yaml
name: Continuous Integration & Security Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Source Code
        uses: actions/checkout@v4

      - name: Setup Java 21 Toolchain
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      # Testing issue: Skipping tests (-x test) on main permits regressions caused by merge commits or semantic conflicts to reach production.
      - name: Build Application Jar
        run: |
          # Fast-track packaging to accelerate release velocity
          ./gradlew bootJar -x test

      - name: Build Docker Image
        run: |
          docker build -t app-service:${{ github.sha }} .

      # Security issue: Suppressing security scanning failures (continue-on-error: true) deploys vulnerable container images to production.
      - name: Trivy Container Vulnerability Scan
        uses: aquasecurity/trivy-action@master
        continue-on-error: true
        with:
          image-ref: 'app-service:${{ github.sha }}'
          format: 'table'
          exit-code: '1'
          ignore-unfixed: true
          severity: 'CRITICAL,HIGH'

      # Maintainability issue: Deploying unchecked images directly without blocking on failed security gates.
      - name: Publish Artifact to Registry
        run: |
          echo "Pushing image to artifact registry..."
```

---

## Discovered Issues

### Testing issue: Skipping Tests on the Main Deployment Branch
Skipping tests with `-x test` on `main` relies on the assumption that "tests already passed on the PR branch". This assumption frequently fails in practice due to:
1. **Semantic Merge Conflicts**: When two independent PRs (PR A and PR B) merge into `main` in sequence, both may pass tests individually on their respective branches, but together introduce runtime collisions, incompatible database queries, or conflicting bean definitions.
2. **Dynamic / Floating Dependencies**: If dependencies use dynamic version ranges or transitive parent POM updates occur between PR merge and deploy, untested code is packaged into the production jar.
3. **Environment and Secret Drift**: CI test configurations or test containers running against staging databases may reveal failures that only manifest during full build runs.
4. Every commit to `main` must produce a verified, fully tested, reproducible release artifact.

### Security issue: Suppressing Container Vulnerability Scanning Failures
Setting `continue-on-error: true` on Trivy, Grype, or Snyk vulnerability scanners renders the entire security scanning stage useless:
- Known CVEs with public exploits (e.g., Log4Shell, Spring4Shell, OpenSSL vulnerabilities, remote command execution bugs in base Alpine/Debian images) will trigger an exit code 1, but GitHub Actions will ignore the error and publish the infected container image directly to the production registry.
- Directly violates SOC2, ISO 27001, and PCI-DSS Requirement 6.3.2 (which requires automated blocking of known critical vulnerabilities prior to release).

---

## Correct Implementation

See [`correct/ci-pipeline.yml`](correct/ci-pipeline.yml) for the production-grade pipeline with strict security gates, parallelized test execution, Gradle build caching, and Trivy blocking enforcement.
