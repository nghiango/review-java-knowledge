# CI/CD Production Operations & Incident Guide

Operational postmortems, deployment telemetry standards, and production readiness checklists for zero-downtime releases.

---

## 1. Production Incident Postmortems

### Incident 1: The Column Drop Catastrophe

#### Incident Timeline
- **10:14 UTC**: CI/CD pipeline for Release $v2.4$ executes `./gradlew flywayMigrate` against the production PostgreSQL database.
- **10:14:02 UTC**: Migration script executes `ALTER TABLE orders DROP COLUMN customer_notes;` without an explicit `lock_timeout`.
- **10:14:05 UTC**: A reporting analytics query holds a read lock on `orders`. The `ALTER TABLE` statement requests an `ACCESS EXCLUSIVE` lock and blocks.
- **10:14:15 UTC**: All subsequent client `SELECT` and `INSERT` queries queue behind the blocked `ALTER TABLE` statement. HikariCP connection pool on all 20 running Spring Boot instances reaches 100% saturation.
- **10:14:30 UTC**: Application Load Balancer reports a 95% spike in HTTP 500 / 504 Gateway Timeout errors.
- **10:16 UTC**: Database administrator manually terminates the blocked DDL lock. The migration completes, and `customer_notes` is dropped.
- **10:16:05 UTC**: Running $v1$ instances resume, but every query referencing `customer_notes` throws `PSQLException: column "customer_notes" does not exist`. 100% of order queries fail continuously for another 4 minutes until $v2$ containers finish rolling out.
- **Total Customer Outage**: **6 minutes of severe business disruption and failed orders**.

#### Root Cause Analysis
1. Destructive DDL was executed *before* application deployment, violating the **Dual-State Coexistence Invariant**.
2. Lack of `lock_timeout` allowed the DDL statement to queue and cascade into connection pool starvation across the entire cluster.
3. No rollback safety: once `customer_notes` was dropped, rolling back to $v1$ was impossible.

#### Permanent Remediation
1. Mandatory adoption of the **Expand-Contract Pattern**: column drops are forbidden in release deployment scripts.
2. Flyway migrations enforce strict lock timeouts in PostgreSQL:
   ```sql
   SET lock_timeout = '2s';
   ```
3. Automated CI linter script scans SQL migration files and fails the build if `DROP COLUMN` or `ALTER TABLE ... RENAME` is detected without an architectural waiver.

---

### Incident 2: The Zombie `:latest` Rollout

#### Incident Timeline
- **16:30 UTC**: CD pipeline deploys the latest build of the Inventory Service, tagging the image as `:latest` in Amazon ECR.
- **16:32 UTC**: New containers boot up, but crash immediately on startup due to a missing environment secret (`REDIS_AUTH_TOKEN`).
- **16:35 UTC**: The deployment fails, and ECS stops replacing tasks. However, 3 failed tasks were replaced, reducing cluster capacity by 30%.
- **16:40 UTC**: On-call engineers attempt to roll back the service to the previous release. Because the previous image was overwritten with `:latest`, engineers do not know which Git commit corresponds to the previously stable image.
- **17:05 UTC**: After 25 minutes of Git commit archaeology and manual emergency builds, the correct previous Git commit is identified, rebuilt, and deployed.

#### Root Cause Analysis
1. Use of mutable `:latest` image tags destroyed the immutable audit trail and made instant rollback impossible.
2. Pipeline lacked automated pre-deployment state capture and automatic rollback triggers.

#### Permanent Remediation
1. Tag every container image strictly with the immutable Git commit SHA (`${GITHUB_SHA::8}`).
2. Updated deployment pipeline to record `PREV_TASK_DEF` and automatically trigger rollback upon service instability.

---

## 2. Key Deployment Telemetry & Metrics

| Metric | Target / SLO | Senior Diagnostic Meaning |
|---|---|---|
| **Deployment Frequency** | Several times per day | High frequency indicates small batch sizes and low integration risk. |
| **Change Failure Rate (CFR)** | $< 5\%$ | Percentage of deployments requiring hotfixes, rollbacks, or incident response. |
| **Mean Time to Restore (MTTR)** | $< 10\text{minutes}$ | Time elapsed from failure detection to automated or manual rollback completion. |
| **HTTP 5xx Rate During Deploy** | $0.00\%$ | Any spike during deployment indicates broken backward compatibility or socket drops. |
| **Canary Latency Drift** | p99 $< 10\%$ vs Baseline | Statistically significant latency degradation triggers automated canary abort. |
| **Container Restart Count** | 0 during rollout | Restarts indicate `CrashLoopBackOff`, OOMKills, or unhandled startup exceptions. |

---

## 3. Senior Production Readiness Checklist

### Pipeline Quality & Security Gates
- [ ] All unit, integration (Testcontainers), and architectural tests (ArchUnit) pass on `main`.
- [ ] Static analysis (Checkstyle/Spotless/Error Prone) passes without warnings or suppressions.
- [ ] Container security scanning (Trivy) runs on every build with zero unresolved `HIGH` or `CRITICAL` CVEs.
- [ ] Container images are tagged strictly with immutable Git commit SHAs; `:latest` is forbidden.
- [ ] Images are cryptographically signed using Sigstore Cosign keyless signing.

### Database Schema Evolution (Zero-Downtime)
- [ ] All database migrations are backward-compatible with the currently running application version.
- [ ] Destructive migrations (`DROP COLUMN`, column renames) follow the multi-phase Expand-Contract pattern.
- [ ] Migration scripts set explicit `lock_timeout` ($\le 3\text{s}$) to prevent connection pool starvation.
- [ ] Indexes are created using `CREATE INDEX CONCURRENTLY` in PostgreSQL.
- [ ] In-app migrations are disabled in production (`spring.flyway.enabled=false`); migrations run as a dedicated pre-deployment step.

### Deployment & Rollback Safety
- [ ] Application implements Spring Boot graceful shutdown (`server.shutdown=graceful`) with aligned deregistration delays.
- [ ] Automated health checks distinguish liveness (`/actuator/health/liveness`) from readiness (`/actuator/health/readiness`).
- [ ] Pipeline captures previous task definition/manifest state before triggering deployments.
- [ ] Synthetic post-deployment smoke tests validate core business paths before declaring deployment success.
- [ ] Automated rollback triggers revert traffic immediately upon smoke test failure or 5xx error spikes.
