# CI/CD Code Review Practice

Review the following continuous integration workflows, deployment pipeline configurations, and database schema migrations. Identify reliability hazards, security regressions, missing rollback safety nets, and zero-downtime violations before expanding the solution panels.

---

## 1. Destructive Database Migration in Deployment Pipeline

An engineering team created this deployment workflow and Flyway migration script to refactor customer names from a single `full_name` column into `first_name` and `last_name` in PostgreSQL.

### Broken Target: `deploy-and-migrate.yml`

--8<-- "modules/26-ci-cd/broken-examples/destructive-db-migration-before-deploy/deploy-and-migrate.yml"

### Broken Target: `V2__drop_old_columns.sql`

--8<-- "modules/26-ci-cd/broken-examples/destructive-db-migration-before-deploy/V2__drop_old_columns.sql"

??? question "Reveal issues"
    1. **Breaking Active $v1$ Application Instances**:
       - Running `./gradlew flywayMigrate` immediately executes `ALTER TABLE customers DROP COLUMN full_name`.
       - For the 3–5 minutes during the rolling update while new containers boot, all existing running containers (version $v1$) continue receiving production traffic.
       - Any query touching `full_name` fails with `PSQLException: column "full_name" does not exist`, triggering an avalanche of HTTP 500 errors.
    2. **Elimination of Rollback Safety**:
       - Dropping `full_name` permanently removes the column and its data.
       - If the incoming $v2$ deployment crashes or exhibits bugs, rolling back to $v1$ is completely impossible because $v1$ cannot operate without `full_name`.
    3. **Violation of the Expand-Contract Pattern**:
       - Schema migrations must be phased: additive changes (Expand) first, followed by data backfill, code transition, and finally column dropping (Contract) weeks later.

---

## 2. Deployment Pipeline Without Rollback Safety

This GitHub Actions workflow handles automated production deployments for an Order Fulfillment service running on AWS ECS Fargate.

### Broken Target: `release.yml`

--8<-- "modules/26-ci-cd/broken-examples/no-rollback-path-pipeline/release.yml"

??? question "Reveal issues"
    1. **Mutable `:latest` Tagging**:
       - Overwriting `:latest` erases the reference to the previously deployed image.
       - Newly autoscaled containers pull different image layers than existing containers, creating an un-auditable split-brain production fleet.
    2. **Missing Deployment Health Gates**:
       - The workflow calls `aws ecs update-service --force-new-deployment` and exits immediately.
       - It does not wait for service stability (`aws ecs wait services-stable`) and does not verify container health.
    3. **No Synthetic Smoke Testing or Automated Rollback**:
       - If new containers crash loop (`CrashLoopBackOff`, OOMKill, or missing secret), the pipeline reports "Success" while production users experience total outage.
       - There is no rollback mechanism to restore the previous task definition revision.

---

## 3. Tests and Security Scanners Skipped on Main Branch

This CI pipeline was configured to optimize build times on the `main` branch.

### Broken Target: `ci-pipeline.yml`

--8<-- "modules/26-ci-cd/broken-examples/tests-and-security-skipped-on-main/ci-pipeline.yml"

??? question "Reveal issues"
    1. **Skipping Tests on Main (`-x test`)**:
       - Relying on the assumption that "tests already passed on the PR branch" fails when multiple PRs merge in sequence, causing semantic merge conflicts, or when floating dependencies introduce regressions on the trunk.
       - Production release jars must always be fully compiled and tested from the exact commit on `main`.
    2. **Suppressing Vulnerability Scan Failures (`continue-on-error: true`)**:
       - Setting `continue-on-error: true` on Trivy allows images with known `CRITICAL` and `HIGH` CVEs (remote code execution, unpatched OpenSSL vulnerabilities) to be deployed directly to production.
       - Directly violates SOC2, ISO 27001, and PCI-DSS Requirement 6.3.2.
    3. **Missing Automated Quality Gates**:
       - Bypasses static analysis and architectural fitness tests (ArchUnit) prior to publishing release artifacts.
