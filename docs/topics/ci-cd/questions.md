# CI/CD & Deployment Interview Questions

Senior Java and backend engineer interview questions covering pipeline architecture, zero-downtime database schema evolution, deployment strategies (Rolling, Blue/Green, Canary), automated rollbacks, and supply chain security.

---

<!-- --8<-- [start:basic] -->
## Basic Concepts (1–8)

### 1. What are the key differences between Continuous Integration (CI), Continuous Delivery (CD), and Continuous Deployment?

At what point does manual human intervention occur in each model?

??? question "Reveal answer"
    - **Continuous Integration (CI)**:
      - Developers merge code changes frequently into the shared trunk (`main`) branch.
      - Every merge triggers an automated pipeline that compiles the code, executes static analysis, and runs unit and integration test suites.
      - *Outcome*: Verifies that the codebase is always green and free of integration bugs.
    - **Continuous Delivery (CD)**:
      - Automatically builds, tests, packages, and prepares deployable release artifacts (Docker images, jar files).
      - Releases are automatically validated in staging/pre-production environments.
      - *Outcome*: The software is guaranteed to be in an instantly releasable state at any moment. **Deployment to production requires manual human approval or a trigger button**.
    - **Continuous Deployment**:
      - Fully automated end-to-end pipeline without human gates. Every commit that passes the automated CI tests and security scans is automatically deployed to production users.
      - Requires high-confidence automated testing, automated rollbacks, and canary validation.

??? example "Example"
    ```yaml
    # GitHub Actions workflow demonstrating a manual approval gate for Continuous Delivery
    deploy-production:
      needs: [test, security-scan]
      runs-on: ubuntu-latest
      environment:
        name: production  # Requires manual reviewer approval in GitHub repository settings
      steps:
        - name: Deploy to ECS
          run: ./scripts/deploy.sh
    ```

---

### 2. What are the essential stages of a Java backend CI pipeline, and why does their execution order matter?

Why should static analysis and unit tests run before container image packaging and end-to-end tests?

??? question "Reveal answer"
    - **Execution Order Rationale (Fail-Fast Principle)**:
      - CI stages must be ordered from **fastest and cheapest** to **slowest and most expensive**.
      - Running static analysis (Checkstyle/Spotless) and unit tests takes $< 2\text{minutes}$. Running Docker builds, pushing multi-gigabyte layers, and running Testcontainers integration tests takes $5-15\text{minutes}$.
      - If code contains a syntax error, formatting flaw, or broken unit test, the build should fail in $< 60\text{seconds}$ without wasting CI compute runner minutes or container registry bandwidth.
    - **Standard CI Stage Sequence**:
      1. *Checkout & Toolchain Setup*: Git clone and JDK 21 installation.
      2. *Compile & Static Analysis*: Fast compiler validation, Spotless formatting, Error Prone.
      3. *Fast Unit Tests*: Mockito and JUnit 5 tests without Docker infrastructure.
      4. *Integration Tests*: Testcontainers verifying PostgreSQL, Kafka, and Redis contracts.
      5. *Container Image Build*: Multi-stage Docker build utilizing Spring Boot layered JARs.
      6. *Security Vulnerability Scanning*: Trivy / Grype scanning for OS and transitive library CVEs.
      7. *Artifact Signing & Publish*: Push signed container image to container registry.

??? example "Example"
    ```yaml
    # Standard fast-feedback job dependency chain
    jobs:
      verify:
        runs-on: ubuntu-latest
        steps:
          - run: ./gradlew check  # Runs spotless, errorprone, and unit tests first
      
      integration-test:
        needs: verify
        runs-on: ubuntu-latest
        steps:
          - run: ./gradlew integrationTest  # Runs slow Testcontainers tests only if verify passes
    ```

---

### 3. Why is tagging container images with `:latest` considered a dangerous anti-pattern in production CD pipelines?

What concrete failure modes occur when deploying with mutable image tags?

??? question "Reveal answer"
    - **Failure Mode 1: Split-Brain Autoscaling**:
      - If an image is tagged `:latest`, newly spawned tasks during horizontal autoscaling pull the newest image layer, while existing tasks run an older binary. The production fleet ends up running mismatched application versions simultaneously.
    - **Failure Mode 2: Inability to Roll Back Deterministically**:
      - If a deployment introduces a critical regression, rolling back requires reverting to the previous working state. With `:latest`, the registry's tag has already been overwritten with the broken code. There is no discrete tag pointing to the previous stable release.
    - **Failure Mode 3: Local Node Docker Cache Skew**:
      - Docker engines cache images by tag. If a host node already has a local `:latest` image cached, it may skip pulling new layers unless `--pull=always` is configured, resulting in stale container execution.
    - **Senior Production Standard**:
      - Every production container image must be tagged with an **immutable identifier**: the short Git commit SHA (`${GITHUB_SHA::8}`) or a semantic release tag (`v2.4.1`).

??? example "Example"
    ```bash
    # Safe immutable tagging pattern
    GIT_SHA=$(git rev-parse --short HEAD)
    IMAGE_NAME="123456789012.dkr.ecr.us-east-1.amazonaws.com/order-service:${GIT_SHA}"
    docker build -t "${IMAGE_NAME}" .
    docker push "${IMAGE_NAME}"
    ```

---

### 4. How does a Rolling Update deployment strategy work, and what are its operational advantages and limitations?

What occurs to in-flight HTTP requests when an old container is replaced?

??? question "Reveal answer"
    - **How Rolling Updates Work**:
      - The container orchestrator (Kubernetes / AWS ECS) replaces container instances incrementally in batches.
      - Configured by `maxSurge` (how many extra instances can be created above target capacity) and `maxUnavailable` (how many instances can be taken down during the rollout).
      - The orchestrator spins up a batch of $v2$ containers, waits for them to pass readiness health checks, registers them with the load balancer, and then sends `SIGTERM` to a corresponding batch of $v1$ containers.
    - **Advantages**:
      - Low infrastructure overhead: requires only a small surge capacity margin (e.g. 25% extra compute) rather than doubling total infrastructure.
    - **Limitations**:
      - **Version Coexistence**: For the entire duration of the rollout ($5-15\text{minutes}$), $v1$ and $v2$ run simultaneously, requiring strict database and API backward compatibility.
      - **Slow Rollback**: Rolling back requires executing a second full rolling update in reverse.

??? example "Example"
    ```yaml
    # Kubernetes Deployment rolling update configuration
    spec:
      replicas: 10
      strategy:
        type: RollingUpdate
        rollingUpdate:
          maxSurge: 25%         # Allow up to 12-13 pods during deployment
          maxUnavailable: 0     # Never drop below 10 running ready pods
    ```

---

### 5. What is a Blue/Green Deployment, and how does it achieve near-instantaneous cutover and rollback?

What are the primary cost and database constraints of Blue/Green architectures?

??? question "Reveal answer"
    - **How Blue/Green Deployments Work**:
      - Two identical production environments exist: **Blue** (currently serving 100% of live production traffic) and **Green** (idle / staging environment).
      - The CD pipeline deploys version $v2$ to the Green environment in complete isolation.
      - Automated synthetic smoke tests run against Green without affecting live users.
      - Once verified, the load balancer or router flips its routing rule to point 100% of incoming traffic to Green.
      - If an issue occurs post-cutover, rolling back takes $< 5\text{seconds}$ by flipping the load balancer back to Blue.
    - **Constraints & Trade-offs**:
      - **Cost**: Doubles compute infrastructure costs during deployments because two full fleets run concurrently.
      - **Database Shared State**: Both Blue and Green must connect to the **same production database**. Therefore, database schema changes must be 100% backward-compatible with both $v1$ and $v2$ simultaneously.

??? example "Example"
    ```mermaid
    flowchart LR
        ALB["Application Load Balancer"]
        subgraph ProductionFleet
            Blue["Blue Environment (v1)<br/>Weight: 0% (Idle / Standby)"]
            Green["Green Environment (v2)<br/>Weight: 100% (Live Traffic)"]
        end
        ALB -->|Flipped to Green| Green
        ALB -.->|Instant Fallback| Blue
    ```

---

### 6. What is a Progressive Canary Deployment, and how does it contain the blast radius of production regressions?

How does canary routing differ between DNS, Load Balancers, and Service Meshes?

??? question "Reveal answer"
    - **How Canary Deployments Work**:
      - Version $v2$ is deployed to a small fraction of the production fleet (e.g., 5% or 10% of total instances or traffic).
      - Automated monitoring evaluates application health (p95 latency, HTTP 5xx error rate, uncaught exception rate) over an observation window (e.g., 10–15 minutes).
      - If metrics remain healthy, traffic gradually increments: $10\% \rightarrow 25\% \rightarrow 50\% \rightarrow 100\%$.
      - If metrics degrade, the canary is immediately aborted and 100% of traffic returns to $v1$.
    - **Blast Radius Containment**:
      - A catastrophic bug (such as a memory leak or uncaught `NullPointerException`) impacts only 5% of users rather than 100% of the customer base.
    - **Traffic Routing Mechanisms**:
      - *Application Load Balancer*: Uses weighted target groups (e.g., Target Group 1 = 90%, Target Group 2 = 10%).
      - *Service Mesh (Istio / Envoy)*: Layer 7 HTTP routing headers (e.g., routing internal beta testers via `Cookie` or `X-User-Type`).
      - *DNS (Route 53)*: Coarse-grained weighted DNS records; limited by client-side DNS caching.

??? example "Example"
    ```hcl
    # AWS ALB weighted routing for 10% canary traffic
    resource "aws_lb_listener_rule" "canary_routing" {
      listener_arn = aws_lb_listener.front_end.arn
      priority     = 10

      action {
        type = "forward"
        forward {
          target_group {
            arn    = aws_lb_target_group.stable_v1.arn
            weight = 90
          }
          target_group {
            arn    = aws_lb_target_group.canary_v2.arn
            weight = 10
          }
        }
      }
    }
    ```

---

### 7. What is the operational distinction between Kubernetes / ECS Liveness and Readiness health probes during a deployment?

What happens if an application marks its readiness probe as healthy before its database connection pool is initialized?

??? question "Reveal answer"
    - **Liveness Probe (`/actuator/health/liveness`)**:
      - *Purpose*: Determines if the application process is deadlocked, hung, or unrecoverable.
      - *Action on Failure*: The orchestrator **kills the container and restarts it**.
      - *Rule*: Must be lightweight. Must NOT check external downstream dependencies (databases, external microservices) to avoid cascading restart storms when downstream dependencies degrade.
    - **Readiness Probe (`/actuator/health/readiness`)**:
      - *Purpose*: Determines if the application container is ready to **accept incoming network traffic**.
      - *Action on Failure*: The orchestrator removes the container from the load balancer target group so no client traffic is routed to it. Does NOT restart the container.
    - **Consequence of Premature Readiness**:
      - If readiness is marked healthy before database pools (HikariCP) or cache connections are validated, the load balancer immediately forwards live user requests to the container.
      - Requests fail with `CannotGetJdbcConnectionException` or `ConnectionTimeoutException`, returning HTTP 500/502 errors to end users during deployment.

??? example "Example"
    ```yaml
    # Spring Boot Actuator probe configuration for Kubernetes
    management:
      health:
        probes:
          enabled: true
      endpoint:
        health:
          show-details: when_authorized
          group:
            readiness:
              include: readinessState, db, redis
            liveness:
              include: livenessState
    ```

---

### 8. Compare Trunk-Based Development with GitFlow in the context of continuous delivery and modern backend engineering.

Why does GitFlow inhibit Continuous Integration and lead to "merge hell"?

??? question "Reveal answer"
    - **GitFlow**:
      - *Model*: Long-lived branches (`develop`, `release`, `hotfix`, and multi-week `feature` branches). Code moves through branching stages before merging to `main`.
      - *Why it Inhibits CI*: Developers isolate changes for days or weeks on separate branches without integrating. When branches finally merge into `develop`, extensive **semantic merge conflicts** arise. Testing long-lived branches in staging delays production release frequency to bi-weekly or monthly release trains.
    - **Trunk-Based Development**:
      - *Model*: All developers merge small, frequent commits directly into a single shared branch (`main` or `trunk`) at least once a day. Short-lived feature branches live $< 1-2\text{days}$.
      - *Why it Enables CI/CD*: Continuous small merges mean integration conflicts are tiny and resolved immediately. Incomplete features are safely hidden behind **Feature Flags** rather than sequestered on unmerged branches.
      - Every commit to `main` is potentially releasable to production.

??? example "Example"
    ```text
    GitFlow (Anti-pattern for CD):
    main ───────────────────────────────● Release v1.0
          \                            /
    develop ──────●────────●──────────●
                   \      /
             feature-branch (3 weeks without integrating)

    Trunk-Based Development (Golden Standard):
    main ──●──────●──────●──────●──────● (Continuous Integration & Release)
            \    /        \    /
           feature A     feature B
           (1 day max)   (1 day max)
    ```

---

<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Pipeline Architecture (9–16)

### 9. Explain the Expand-Contract (Parallel Change) pattern for database schema migrations.

How do you safely rename a column `full_name` to `customer_name` across millions of rows without downtime?

??? question "Reveal answer"
    - **Step 1: Expand (Release 1)**:
      - Add the new column `customer_name` as **nullable**:
        `ALTER TABLE customers ADD COLUMN customer_name VARCHAR(255);`
      - Add a database trigger (or dual-write logic in the application) so any write from active $v1$ code updates both `full_name` and `customer_name`.
      - Both $v1$ and $v2$ continue operating cleanly without missing-column exceptions.
    - **Step 2: Backfill (Asynchronous Batch Job)**:
      - Execute an asynchronous background migration in chunked batches (e.g. 5,000 rows per transaction) to copy historical data from `full_name` to `customer_name`:
        `UPDATE customers SET customer_name = full_name WHERE customer_name IS NULL AND id BETWEEN ? AND ?;`
    - **Step 3: Transition (Release 2)**:
      - Deploy version $v2$ of the backend code: $v2$ reads and writes strictly from `customer_name`.
      - If $v2$ fails, rollback to $v1$ is completely safe because $v1$'s column `full_name` is still intact and synchronized.
    - **Step 4: Contract (Release 3)**:
      - Weeks later, after $v2$ has proven completely stable, deploy a migration removing the trigger and dropping the legacy column:
        `ALTER TABLE customers DROP COLUMN full_name;`

??? example "Example"
    ```sql
    -- Phase 1 Expand: Add column and synchronization trigger
    ALTER TABLE customers ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);

    CREATE OR REPLACE FUNCTION sync_customer_name()
    RETURNS TRIGGER AS $$
    BEGIN
        IF NEW.customer_name IS NULL THEN
            NEW.customer_name := NEW.full_name;
        END IF;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;

    CREATE OR REPLACE TRIGGER trg_sync_customer_name
    BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION sync_customer_name();
    ```

---

### 10. Why is running Flyway or Liquibase migrations inside application startup code (`spring.flyway.enabled=true`) hazardous in autoscaled production environments?

What failure mode occurs when 30 container replicas boot up simultaneously during a rolling update?

??? question "Reveal answer"
    - **The Multi-Pod Lock Contention Crisis**:
      - When 30 new container replicas launch simultaneously during an ECS or Kubernetes rolling deployment, each Spring Boot container executes Flyway on its main startup thread.
      - All 30 instances attempt to acquire an exclusive lock on the `flyway_schema_history` table:
        `SELECT * FROM flyway_schema_history FOR UPDATE;`
      - One pod wins the lock and begins executing migrations. The remaining 29 pods block indefinitely on database lock acquisition.
    - **Cascading Container Startup Timeouts**:
      - While blocked waiting for the database lock, the container cannot start Tomcat or respond to orchestrator readiness probes.
      - The orchestrator reaches its container `initialDelaySeconds` or `timeoutSeconds` and marks the 29 pods unhealthy, terminating them and spinning up 29 replacement pods.
      - This creates an unrecoverable **startup failure loop** that saturates database connections and crashes the deployment.
    - **Senior Best Practice**:
      - Set `spring.flyway.enabled=false` in production application images.
      - Execute migrations as a **single dedicated pre-deployment step** in the CI/CD pipeline (or via a Kubernetes `Job` or ArgoCD `PreSync` hook) *before* rolling out application containers.

??? example "Example"
    ```yaml
    # application-prod.yml: Disable in-app migrations in production
    spring:
      flyway:
        enabled: false
    ```
    ```yaml
    # CI/CD deployment pipeline executes migrations once as a dedicated job
    - name: Run Schema Migrations
      run: |
        ./gradlew flywayMigrate -Pflyway.url="${PROD_DB_URL}"
    ```

---

### 11. How do Feature Flags decouple software deployment from feature release, and how do you prevent technical debt accumulation?

What architectural pattern prevents feature flag checks from cluttering domain business logic?

??? question "Reveal answer"
    - **Decoupling Deployment from Release**:
      - *Deployment*: The physical act of transferring compiled binaries and running them on production hardware.
      - *Release*: The business decision to activate the feature for end users.
      - By wrapping new code paths in feature flags (LaunchDarkly, Unleash), code can be deployed to production days or weeks ahead of launch without exposing it to customers.
    - **Operational Kill Switch**:
      - If a released feature introduces unexpected memory leaks, downstream API throttling, or database lock contention, the feature flag can be toggled to `OFF` in $< 1\text{second}$ across all production instances without triggering a code rollback or deployment.
    - **Architectural Isolation (Strategy Pattern)**:
      - Never litter domain services with nested `if (flag) { ... } else { ... }` blocks.
      - Use the **Strategy Pattern** with Spring dependency injection:
        ```java
        public interface PaymentProcessor {
            PaymentResult process(PaymentCommand cmd);
        }
        ```
        A routing decorator or factory inspects the feature flag and delegates cleanly to `V1PaymentProcessor` or `V2PaymentProcessor`.
    - **Technical Debt Retirement**:
      - When creating a feature flag, simultaneously file a ticket in the backlog to delete the flag and decommission legacy code paths 2 sprints after 100% rollout.

??? example "Example"
    ```java
    @Service
    public class RoutingPaymentService implements PaymentService {
        private final PaymentService legacyService;
        private final PaymentService stripeService;
        private final FeatureFlagClient featureFlags;

        public RoutingPaymentService(LegacyPaymentService legacy, StripePaymentService stripe, FeatureFlagClient flags) {
            this.legacyService = legacy;
            this.stripeService = stripe;
            this.featureFlags = flags;
        }

        @Override
        public PaymentReceipt charge(PaymentRequest request) {
            if (featureFlags.isEnabled("stripe-v2-integration", request.customerId())) {
                return stripeService.charge(request);
            }
            return legacyService.charge(request);
        }
    }
    ```

---

### 12. How do tools like Spotless, Checkstyle, and ArchUnit automate architectural compliance in continuous integration pipelines?

Write an ArchUnit test that fails the build if a Controller accesses a Repository directly.

??? question "Reveal answer"
    - **Spotless**:
      - Enforces automated source code formatting (e.g., Google Java Format, AOSP conventions) and import ordering. Fails the build if code deviates from agreed style rules, eliminating nitpicking in code reviews.
    - **Checkstyle & Error Prone**:
      - Catch static programming mistakes at compile-time: unused variables, missing `@Override` annotations, unchecked exceptions, or unsafe type conversions.
    - **ArchUnit**:
      - Unit testing framework for Java code architecture. Analyzes bytecode to enforce architectural boundaries, hexagonal/onion architecture invariants, and package dependency rules.
      - Executes directly as a standard JUnit 5 test during `./gradlew test`.

??? example "Example"
    ```java
    @AnalyzeClasses(packages = "lab.orderservice", importOptions = ImportOption.DoNotIncludeTests.class)
    public class ArchitectureRulesTest {

        @ArchTest
        public static final ArchRule controllers_must_not_access_repositories =
            noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("Controllers must delegate to Service layer and never touch Repositories directly");
    }
    ```

---

### 13. How does container vulnerability scanning (Trivy / Grype) and Software Bill of Materials (SBOM) generation work in enterprise CI pipelines?

Why is scanning the base Docker image as critical as scanning Java Maven/Gradle dependencies?

??? question "Reveal answer"
    - **The Two-Tier Vulnerability Surface**:
      1. *Application Dependencies*: Vulnerabilities in open-source Java libraries packaged inside the fat jar (e.g., Jackson deserialization CVEs, Spring Framework vulnerabilities, Log4Shell).
      2. *Base Operating System*: Vulnerabilities in native OS libraries packaged inside the container base image (e.g., Debian/Ubuntu/Alpine C-libraries, `glibc`, OpenSSL, `curl`, `busybox`).
    - **Why Base Image Scanning Matters**:
      - A Java application may have zero vulnerable Maven dependencies, but if built on an outdated base image (e.g. `FROM openjdk:11` or unpatched Ubuntu), an attacker exploiting a local privilege escalation or remote code execution flaw in `glibc` can compromise the host node.
    - **Trivy Execution Invariant**:
      - Scans both OS packages and language-specific dependency locks (`build.gradle.kts` / `pom.xml`).
      - In CI, configure:
        `--exit-code 1 --severity CRITICAL,HIGH --ignore-unfixed`
      - This fails the pipeline only if actionable patches exist for critical/high vulnerabilities.
    - **SBOM (Software Bill of Materials)**:
      - Tools like `syft` generate a cryptographically signed CycloneDX/SPDX manifest cataloging all packages and versions, providing an immutable audit trail for security compliance.

??? example "Example"
    ```yaml
    # Trivy container scan step in GitHub Actions
    - name: Run Trivy Vulnerability Scanner
      uses: aquasecurity/trivy-action@master
      with:
        image-ref: 'my-app:${{ github.sha }}'
        format: 'table'
        exit-code: '1'
        ignore-unfixed: true
        severity: 'CRITICAL,HIGH'
    ```

---

### 14. Why must PostgreSQL DDL migrations always set an explicit `lock_timeout`, and why should indexes be created with `CONCURRENTLY`?

What catastrophe occurs in high-traffic tables if `lock_timeout` is omitted?

??? question "Reveal answer"
    - **The Lock Queue Starvation Catastrophe**:
      - Executing `ALTER TABLE orders ADD COLUMN ...` requires an `ACCESS EXCLUSIVE` lock in PostgreSQL.
      - If an existing long-running query or payment transaction is reading from `orders`, the `ALTER TABLE` statement must wait for that query to finish.
      - In PostgreSQL, **all subsequent queries queue behind the waiting DDL statement**!
      - Within seconds, hundreds of incoming HTTP requests attempt to query `orders` and block in the lock queue.
      - The HikariCP connection pool exhausts all connections in $< 5\text{seconds}$, triggering a complete cascading application outage.
    - **Senior Prevention Standards**:
      1. **Set Explicit `lock_timeout`**:
         ```sql
         SET lock_timeout = '3s';
         ALTER TABLE orders ADD COLUMN ...;
         ```
         If the lock cannot be acquired within 3 seconds, PostgreSQL immediately aborts the migration rather than blocking incoming application traffic.
      2. **Use `CREATE INDEX CONCURRENTLY`**:
         Standard `CREATE INDEX` locks the table against writes (`SHARE` lock). `CREATE INDEX CONCURRENTLY` builds the index in two passes without acquiring write locks, preserving production write throughput.

??? example "Example"
    ```sql
    -- Safe production migration script for PostgreSQL
    SET lock_timeout = '2s';
    SET statement_timeout = '30s';

    -- Non-blocking index creation (must execute outside transactional DDL block)
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
    ```

---

### 15. How do automated deployment rollback mechanisms work when integrated with synthetic smoke tests and metrics?

What criteria should trigger an automated rollback within the first 5 minutes of deployment?

??? question "Reveal answer"
    - **Automated Rollback Architecture**:
      - A deployment must never be considered complete when the container process starts. It requires post-deployment verification.
      - The CD pipeline provisions the new version ($v2$) and initiates an evaluation timer (e.g. 5–10 minutes).
    - **Rollback Triggers**:
      1. **Synthetic Smoke Test Failure**: The pipeline executes synthetic API calls against internal `/actuator/health/readiness` and core business transaction endpoints. Any non-200 response triggers an instant rollback.
      2. **HTTP 5xx Spike**: Application Load Balancer `HTTPCode_Target_5XX_Count` exceeding a predefined error budget threshold ($> 1\%$ of requests).
      3. **Latency Anomaly**: p99 response time exceeding $3\times$ baseline latency.
      4. **Container Crash / OOMKill**: Rapid container restart loops (`CrashLoopBackOff` or Exit Code 137).
    - **Execution**:
      - In ECS: Re-register the previous known-good task definition revision.
      - In Blue/Green: Flip the load balancer target weights back to the Blue environment.
      - In Kubernetes: Run `kubectl rollout undo deployment/<name>`.

??? example "Example"
    ```bash
    # Post-deployment synthetic smoke test and automated rollback script
    if ! curl -sf https://api.prod.company.internal/actuator/health/readiness; then
        echo "Readiness probe failed! Initiating automated rollback..."
        aws ecs update-service --cluster prod --service orders --task-definition "$PREVIOUS_TASK_DEF"
        exit 1
    fi
    ```

---

### 16. How does cryptographic artifact signing with Sigstore Cosign guarantee container supply chain security?

How does keyless signing using OpenID Connect (OIDC) eliminate the danger of compromised private signing keys?

??? question "Reveal answer"
    - **The Threat Model (Supply Chain Tampering)**:
      - Attackers who compromise a container registry or CI/CD runner can overwrite a valid image tag with a malicious image containing backdoors or cryptocurrency miners.
      - Without cryptographic signing, deployment environments cannot verify if an image was truly built by their authorized CI pipeline.
    - **How Sigstore Cosign Works**:
      - During CI, Cosign computes the cryptographic digest (SHA-256) of the built container image and signs it.
      - The signature is published directly to the container registry alongside the image as an OCI artifact.
    - **Keyless Signing via OIDC**:
      - Traditional signing requires managing private PGP/RSA keys stored in CI repository secrets (which can be leaked or stolen).
      - In **Keyless Signing**, Cosign authenticates with the CI runner's **OpenID Connect (OIDC)** identity provider (e.g., GitHub Actions token).
      - A short-lived Certificate Authority (Fulcio) issues a temporary X.509 certificate valid for only 10 minutes tied to the specific GitHub repository and workflow.
      - The signature is recorded in a public tamper-proof transparency ledger (Rekor).
      - In production, Kubernetes admission controllers (Kyverno) verify the signature and certificate identity before admitting the container to run.

??? example "Example"
    ```yaml
    # Cosign keyless signing in GitHub Actions
    - name: Sign container image
      run: |
        cosign sign --yes 123456789012.dkr.ecr.us-east-1.amazonaws.com/order-service:${{ github.sha }}
    ```

---

<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Production Engineering (17–21)

### 17. How does the GitOps deployment paradigm (ArgoCD / Flux) differ from traditional push-based CI/CD pipelines?

Explain reconciliation loops, declarative drift detection, and why production access credentials should not live in CI runners.

??? question "Reveal answer"
    - **Push-Based Pipelines vs Pull-Based GitOps**:
      - *Push-Based (Jenkins / GitHub Actions)*: The CI runner builds the artifact and uses stored long-lived production credentials (`AWS_ACCESS_KEY_ID`, `KUBECONFIG`) to push deployments directly into production clusters. If the CI runner is compromised, attackers obtain administrative control over production.
      - *Pull-Based GitOps (ArgoCD)*: CI only builds images and pushes a commit to a Git repository containing declarative manifests (e.g., updating `image: v2.1.0` in a Helm/Kustomize repo). An internal controller running *inside* the production cluster monitors Git and pulls changes. No production credentials ever leave the cluster boundary.
    - **Reconciliation Loops & Drift Detection**:
      - The GitOps controller continuously compares the **desired state** (declared in Git) with the **live state** (running in Kubernetes etcd).
      - If an engineer manually edits a deployment or modifies replica counts (`kubectl scale`), the controller detects the architectural drift and automatically reconciles the cluster back to match Git.
    - **Sync Waves & Hooks**:
      - ArgoCD executes deployments in structured phases (PreSync database migrations $\rightarrow$ Sync application rollout $\rightarrow$ PostSync smoke tests).

??? example "Example"
    ```yaml
    # ArgoCD Application manifest defining GitOps reconciliation
    apiVersion: argoproj.io/v1alpha1
    kind: Application
    metadata:
      name: order-service
    spec:
      source:
        repoURL: 'https://github.com/company/k8s-manifests.git'
        targetRevision: HEAD
        path: apps/order-service/overlays/production
      destination:
        server: 'https://kubernetes.default.svc'
        namespace: production
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
    ```

---

### 18. Why do advanced progressive canary systems use non-parametric statistical hypothesis testing (e.g., Mann-Whitney U Test) rather than simple threshold comparisons?

How does statistical testing prevent false-positive rollbacks caused by temporary traffic anomalies?

??? question "Reveal answer"
    - **The Flaw of Simple Thresholds**:
      - Comparing simple averages (e.g., "Roll back if Canary p95 latency $> 500\text{ms}$") is brittle:
        - If an external third-party payment gateway experiences a transient 2-second timeout affecting 3 requests across the entire cluster, the canary average latency spikes, triggering a false-positive rollback.
        - Conversely, if traffic is light, a sample size of 20 requests has high variance and cannot reliably prove that a new build is safe.
    - **Non-Parametric Statistical Testing (Mann-Whitney U / Wilcoxon)**:
      - Used by systems like Kayenta, Argo Rollouts, and Flagger.
      - Collects metric distributions from both the Baseline ($v1$) and Canary ($v2$) over identical time slices.
      - Does not assume a normal (Gaussian) distribution, making it ideal for skewed latency distributions.
      - Calculates the probability ($p$-value) that differences between Baseline and Canary occurred purely by random chance.
      - Rollback triggers only when statistical confidence exceeds 95% ($p < 0.05$) that the Canary distribution is genuinely degraded compared to the Baseline, preventing costly deployment churn.

??? example "Example"
    ```yaml
    # Argo Rollouts AnalysisTemplate using Prometheus metrics
    apiVersion: argoproj.io/v1alpha1
    kind: AnalysisTemplate
    metadata:
      name: success-rate
    spec:
      metrics:
      - name: success-rate
        interval: 30s
        successCondition: result[0] >= 0.99
        failureLimit: 3
        provider:
          prometheus:
            address: http://prometheus:9090
            query: |
              sum(rate(http_requests_total{status!~"5.*",app="canary"}[1m])) 
              / 
              sum(rate(http_requests_total{app="canary"}[1m]))
    ```

---

### 19. How do you orchestrate load balancer connection draining with Spring Boot's graceful shutdown lifecycle during zero-downtime rolling deployments?

Explain the exact mathematical relationship between deregistration delay, shutdown phase timeout, and container stop timeout.

??? question "Reveal answer"
    - **The Sequence of Events During Container Termination**:
      1. The orchestrator calls `DeregisterTargets` on the load balancer.
      2. The load balancer marks the target as `draining`: it stops sending new incoming requests, but allows in-flight requests to complete for the duration of the **Deregistration Delay**.
      3. Concurrently, the orchestrator sends a `SIGTERM` signal to the container process.
      4. Spring Boot initiates graceful shutdown (`server.shutdown=graceful`): Tomcat stops accepting new TCP connections and gives active worker threads up to `timeout-per-shutdown-phase` to finish executing.
      5. After graceful shutdown completes, the JVM exits cleanly (Exit Code 0).
      6. If the container is still running after `stopTimeout` (ECS) or `terminationGracePeriodSeconds` (K8s), the orchestrator issues `SIGKILL` (Exit Code 137).
    - **The Synchronization Invariant**:
      $$\text{Stop Timeout (K8s / ECS)} > \text{ALB Deregistration Delay} \ge \text{Spring Graceful Shutdown Timeout} + \text{Max Request Latency}$$
    - **Example Production Sizing**:
      - Max expected HTTP request duration: $5\text{seconds}$
      - Spring Boot `spring.lifecycle.timeout-per-shutdown-phase`: $25\text{seconds}$
      - ALB `deregistration_delay.timeout_seconds`: $30\text{seconds}$
      - Orchestrator `stopTimeout` / `terminationGracePeriodSeconds`: $40\text{seconds}$

??? example "Example"
    ```properties
    # application.properties: Spring Boot Graceful Shutdown
    server.shutdown=graceful
    spring.lifecycle.timeout-per-shutdown-phase=25s
    ```
    ```hcl
    # Terraform ALB Target Group Deregistration Delay
    resource "aws_lb_target_group" "app" {
      port                 = 8080
      protocol             = "HTTP"
      deregistration_delay = 30
    }
    ```

---

### 20. How do you maintain backward and forward API compatibility during continuous deployment of microservices?

Explain Consumer-Driven Contract Testing (Pact) and additive API versioning rules.

??? question "Reveal answer"
    - **Additive API Versioning Rules**:
      1. *Never remove or rename an existing field* in request/response JSON payloads.
      2. *Never change the data type* of an existing field (e.g. changing string to integer).
      3. *Always add new fields as optional/nullable*.
      4. Client JSON deserializers (Jackson) must be configured with:
         `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false`
         so clients ignore newly added fields without breaking.
    - **Consumer-Driven Contract Testing (Pact)**:
      - In microservices, end-to-end integration tests in CI are notoriously slow and flaky.
      - With Pact:
        - The **Consumer** defines a contract specifying the exact request payload it sends and the minimum response attributes it expects.
        - The contract is published to a shared **Pact Broker**.
        - In the **Provider's** CI pipeline, an automated test replays all published consumer contracts against the provider code.
        - If a proposed provider change breaks any active consumer's expectations, the provider build fails before deployment (`can-i-deploy` verification gate).

??? example "Example"
    ```java
    // Configuring Jackson for backward compatibility
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }
    ```

---

### 21. How does OpenID Connect (OIDC) token federation eliminate static credentials in CI/CD runners?

Compare static repository secrets with temporary dynamic STS credentials for AWS deployments.

??? question "Reveal answer"
    - **The Hazard of Static CI Repository Secrets**:
      - Storing `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in GitHub Actions repository secrets creates permanent, high-privilege credentials.
      - If a developer prints secrets in a debug log, or a malicious dependency or compromised pull request action exfiltrates environment variables, attackers obtain permanent unauthorized access to production cloud infrastructure.
      - Static keys require manual rotation and audit overhead.
    - **OIDC Token Federation**:
      1. An IAM Identity Provider is configured in AWS trusting `token.actions.githubusercontent.com`.
      2. An IAM Role is created with a Trust Policy restricting the `aud` to `sts.amazonaws.com` and the `sub` claim to the specific repository and branch:
         `repo:company/order-service:ref:refs/heads/main`
      3. When the GitHub Actions job runs, GitHub issues a short-lived, cryptographically signed OIDC JWT token.
      4. The runner passes this token to AWS STS:
         `sts:AssumeRoleWithWebIdentity`
      5. AWS validates the token signature and returns temporary AWS credentials valid for only 1 hour.
      6. No static keys exist to be leaked, rotated, or stolen.

??? example "Example"
    ```yaml
    # GitHub Actions configuring OIDC role assumption without static secrets
    jobs:
      deploy:
        runs-on: ubuntu-latest
        permissions:
          id-token: write  # Mandatory for requesting GitHub OIDC JWT
          contents: read
        steps:
          - name: Configure AWS Credentials via OIDC
            uses: aws-actions/configure-aws-credentials@v4
            with:
              role-to-assume: arn:aws:iam::123456789012:role/github-actions-deploy-role
              aws-region: us-east-1
    ```

---

<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incident Scenarios (22–23)

### 22. Production Incident: A CI/CD deployment pipeline executes `ALTER TABLE orders DROP COLUMN customer_notes` as part of a pre-deployment Flyway migration. Immediately, active production instances throw an avalanche of HTTP 500 errors. What happened and how do you resolve it?

Walk through the immediate operational triage, root cause analysis, and permanent pipeline remediation.

??? question "Reveal answer"
    - **Incident Walkthrough**:
      - At 10:15 UTC, the CI/CD pipeline triggered for release $v2.0$.
      - The pipeline ran `./gradlew flywayMigrate` against the production PostgreSQL database.
      - The migration executed: `ALTER TABLE orders DROP COLUMN customer_notes;`
      - Immediately, the Application Load Balancer reported a spike in HTTP 500 Internal Server Errors ($> 80\%$ error rate).
      - Active running instances (version $v1$) began throwing `org.postgresql.util.PSQLException: ERROR: column "customer_notes" does not exist` across all order checkout and query endpoints.
      - Because the rolling update takes 4 minutes to spin up new containers, production remained hard down for the entire rollout window.
    - **Immediate Operational Triage**:
      1. Reverting code to $v1$ is **impossible** because the column and historical data were destroyed.
      2. If $v2$ is already packaged and ready, immediately expedite the rollout of $v2$ to replace all $v1$ instances as fast as possible.
      3. If $v2$ is not stable, execute emergency DDL restoring the column as nullable:
         `ALTER TABLE orders ADD COLUMN customer_notes TEXT;`
         This allows $v1$ queries to succeed (with empty data) while recovering service availability.
    - **Root Cause Analysis**:
      - The engineering team violated the **Dual-State Coexistence Invariant**.
      - During rolling updates, $v1$ and $v2$ always run concurrently. A destructive schema change breaks active $v1$ code before $v2$ can take over.
    - **Permanent Remediation**:
      - Enforce the **Expand-Contract Pattern** via CI linters and code review checklists:
        - Never allow `DROP COLUMN`, column renames, or synchronous `NOT NULL` additions in the same release as code changes.
        - Decommission columns only after the code using them has been out of production for at least one full release cycle.

??? example "Example"
    ```sql
    -- Emergency triage query to restore backward compatibility for v1
    ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_notes TEXT DEFAULT '';
    ```

---

### 23. Production Incident: An in-place rolling update deploys a new Spring Boot service revision. The containers pass the readiness health check, but customers immediately experience 100% checkout failure because a third-party payment API secret was misconfigured in the deployment manifest. The pipeline reported "success". How do you design an automated deployment health gate and rollback safety net?

Walk through the diagnostic postmortem and the construction of an automated rollback pipeline.

??? question "Reveal answer"
    - **Incident Walkthrough**:
      - A developer updated the payment service container definition with a typo in the secret name (`STRIPE_SECRT_KEY`).
      - The container started up, and Spring Boot initialized successfully.
      - The basic readiness probe (`/actuator/health/readiness`) checked database connectivity and reported `UP` (HTTP 200).
      - The rolling update proceeded to terminate all older, working containers.
      - The pipeline finished and reported "SUCCESS".
      - However, the moment users attempted checkouts, the application threw `NullPointerException` or `AuthenticationException: Stripe secret key is null or invalid`. 100% of customer orders failed.
    - **Root Cause Analysis**:
      1. **Shallow Readiness Check**: The health probe only checked database liveness; it did not validate that critical secrets or configuration properties were populated.
      2. **Missing Post-Deployment Synthetic Smoke Testing**: The CD pipeline did not execute an end-to-end synthetic payment verification request before declaring deployment success.
      3. **Lack of Automated Rollback Gates**: The pipeline lacked an automated rollback trigger tied to application error rates or smoke test results.
    - **Remediation & Permanent Architecture**:
      1. **Fail-Fast Startup Validation**:
         Use `@ConfigurationProperties` with Jakarta Validation (`@NotBlank`, `@NotNull`):
         ```java
         @ConfigurationProperties("stripe")
         @Validated
         public record StripeProperties(@NotBlank String secretKey) {}
         ```
         If the secret is missing or blank, Spring Boot crashes on boot (`ApplicationContextException`), preventing the container from passing readiness checks.
      2. **Post-Deployment Synthetic Smoke Testing in Pipeline**:
         Immediately after deployment stabilization, execute an authenticated synthetic test charge against the internal API.
      3. **Automated Rollback Trigger**:
         If the synthetic test fails, the pipeline automatically re-deploys the previous stable task definition revision and halts the build with an alert to PagerDuty.

??? example "Example"
    ```yaml
    # Production CD pipeline with automated synthetic verification and rollback
    - name: Run Synthetic Post-Deployment Verification
      id: smoke_test
      continue-on-error: true
      run: |
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
          https://api.internal/api/orders/synthetic-check \
          -H "Authorization: Bearer ${{ secrets.SMOKE_TEST_TOKEN }}")
        if [ "$HTTP_CODE" -ne 200 ]; then
          echo "Synthetic check failed with HTTP $HTTP_CODE"
          exit 1
        fi

    - name: Execute Automated Rollback on Smoke Failure
      if: steps.smoke_test.outcome != 'success'
      run: |
        echo "Rolling back to previous stable task definition: $PREV_TASK_DEF"
        aws ecs update-service --cluster prod --service orders --task-definition "$PREV_TASK_DEF"
        aws ecs wait services-stable --cluster prod --services orders
        exit 1
    ```

<!-- --8<-- [end:scenarios] -->
