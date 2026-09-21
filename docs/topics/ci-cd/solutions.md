# CI/CD Solutions & Zero-Downtime Patterns

Detailed engineering walkthroughs for correcting the broken pipeline, deployment, and database migration targets identified in [Code Review](code-review.md).

---

## 1. Zero-Downtime Database Migration: The Expand Phase

### Correct Implementation: `V2__expand_customer_name_columns.sql`

--8<-- "modules/26-ci-cd/broken-examples/destructive-db-migration-before-deploy/correct/V2__expand_customer_name_columns.sql"

### Correct Deployment Workflow: `deploy-safe.yml`

--8<-- "modules/26-ci-cd/broken-examples/destructive-db-migration-before-deploy/correct/deploy-safe.yml"

### Architectural Rationale & Trade-offs
1. **Additive Schema Changes**:
   - The migration adds `first_name` and `last_name` as nullable columns without `NOT NULL DEFAULT ''`.
   - Running $v1$ application instances can continue inserting and querying rows without specifying values for the new columns.
2. **Synchronization Trigger**:
   - The PL/pgSQL trigger automatically extracts first and last names from `full_name` for any write executed by $v1$ instances.
   - Incoming $v2$ instances can read populated data immediately.
3. **Rollback Safety**:
   - If the $v2$ application deployment fails and rolls back to $v1$, $v1$ continues operating without any missing-column errors, because `full_name` is untouched.
4. **Post-Deployment Contracting**:
   - Weeks later, after $v2$ is proven completely stable, a final migration drops the trigger and the legacy `full_name` column.

---

## 2. Immutable Tagging, Stability Gates, and Automated Rollback

### Correct Implementation: `release.yml`

--8<-- "modules/26-ci-cd/broken-examples/no-rollback-path-pipeline/correct/release.yml"

### Architectural Rationale & Trade-offs
1. **Immutable Git SHA Image Tagging**:
   - Every build produces an image tagged with the exact 8-character Git commit hash (`${GITHUB_SHA::8}`).
   - Ensures deterministic autoscaling, traceability from running container to Git source code, and eliminates Docker layer cache skew.
2. **Pre-Deployment State Capture**:
   - The pipeline queries and saves the current stable task definition revision (`PREV_TASK_DEF`) before initiating changes.
3. **Deployment Stability Gate**:
   - Uses `aws ecs wait services-stable` to block pipeline completion until new tasks pass readiness probes and old tasks have cleanly drained.
4. **Automated Rollback Net**:
   - Executes synthetic smoke tests against `/actuator/health/readiness`. If either deployment stabilization or smoke testing fails, the pipeline immediately rolls back to `PREV_TASK_DEF` and exits with an error.

---

## 3. Strict CI Quality & Security Gates

### Correct Implementation: `ci-pipeline.yml`

--8<-- "modules/26-ci-cd/broken-examples/tests-and-security-skipped-on-main/correct/ci-pipeline.yml"

### Architectural Rationale & Trade-offs
1. **Zero Skipped Tests on Trunk**:
   - Every commit to `main` executes static analysis, ArchUnit rules, and unit tests via `./gradlew check`. Catches regressions introduced by sequential PR merges or dependency changes.
2. **Strict Blocking Security Scans**:
   - Trivy runs without `continue-on-error: true`. Builds containing unpatched `HIGH` or `CRITICAL` vulnerabilities fail immediately, preventing deployment to production.
3. **Parallelized Job Architecture**:
   - Tests and container builds run in distinct pipeline jobs. Test reports are uploaded as artifacts for developer diagnostics.
