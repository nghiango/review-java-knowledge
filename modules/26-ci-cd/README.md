# Module 26 — CI/CD

This is a **doc module** covering Continuous Integration, Continuous Delivery, and zero-downtime deployment engineering for Senior Java / Spring Boot Backend Engineers:
Pipeline architecture (compile, test, static analysis with Sonar/Checkstyle, container vulnerability scanning with Trivy/Grype, immutable artifact publishing, and OCI image signing with Cosign), database migrations in deployment pipelines (Flyway/Liquibase execution boundaries, schema locking, and the Expand-Contract / Parallel Change pattern for zero-downtime compatibility), modern deployment strategies (Rolling Updates with termination grace periods, Blue/Green switching via load balancers, and Progressive Canary rollouts with automated metric analysis), automated rollback mechanisms and deployment health gates, feature flag lifecycles and kill switches (LaunchDarkly / Unleash / Spring profiles), and GitOps deployment workflows (ArgoCD, declarative sync, drift detection).

The canonical prose, architecture blueprints, interview Q&A, and operational incident guides live in the documentation:

👉 **[CI/CD Documentation](../../docs/topics/ci-cd/index.md)**

## Broken Review Examples

This module provides 3 realistic broken review targets under `broken-examples/`:

1. `destructive-db-migration-before-deploy/` — CI/CD pipeline running a destructive Flyway schema migration (`DROP COLUMN legacy_status` or synchronous column rename) before deploying the new application version, causing active production instances running $v1$ code to fail catastrophically with database column missing exceptions.
2. `no-rollback-path-pipeline/` — GitHub Actions deployment pipeline executing an in-place direct container replacement tagging images as mutable `:latest`, omitting pre-deployment health gates, automated smoke tests, and rollback orchestration, leaving the system in a broken state when the deployment fails.
3. `tests-and-security-skipped-on-main/` — CI pipeline configuration with `-x test` and `continue-on-error: true` on critical security scanning and static analysis jobs on the `main` branch to "accelerate deployment velocity", allowing failing tests and critical CVEs to be deployed to production.
