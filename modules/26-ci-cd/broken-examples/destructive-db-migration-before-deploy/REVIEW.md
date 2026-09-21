# Code Review: Database Migration & Deployment Pipeline

## Pull Request Description
This PR refactors our customer domain model to separate `full_name` into `first_name` and `last_name` in PostgreSQL.
The PR contains:
- `V2__drop_old_columns.sql` — Flyway migration script adding the new columns and dropping the old `full_name` column to ensure clean database schema hygiene.
- `deploy-and-migrate.yml` — GitHub Actions deployment pipeline that runs `./gradlew flywayMigrate` against production RDS first, followed by triggering an ECS rolling update.

The author tested this locally and verified that after running the migration and starting the new application version, the customer endpoints return first and last names correctly.

## Files Under Review
- `deploy-and-migrate.yml` — GitHub Actions deployment workflow.
- `V2__drop_old_columns.sql` — Flyway database schema migration.

## Review Questions
1. What happens to active production application instances (running version $v1$) during the 3–5 minute rolling deployment window when Flyway executes?
2. If the new application version fails health checks or throws runtime exceptions during deployment, what happens if the team triggers an automated rollback to version $v1$?
3. How must database schema changes be restructured using the **Expand-Contract / Parallel Change pattern** to ensure zero downtime and backward compatibility?
