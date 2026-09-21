# Code Review: ECS Fargate Task Definition for Payment Service

## Pull Request Description
This PR updates the Amazon ECS Task Definition for our core Payment Processing microservice deployed to production on AWS Fargate.
Changes included:
- Updated container image tag to `v2.1.0`.
- Configured production database connection parameters (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- Added Stripe live secret key and JWT signing secret so the Spring Boot container can authenticate payment transactions and validate bearer tokens on startup.

The author reports that the container boots up successfully in the staging ECS cluster and connects to the Aurora PostgreSQL database.

## Files Under Review
- `task-definition.json` — Amazon ECS task definition registration template.

## Review Questions
1. How does ECS handle values defined in the `environment` array, and who has visibility into them?
2. What are the security and compliance risks (PCI-DSS, SOC2) of storing production credentials in task definition revisions?
3. How should secrets be externalized in ECS using native AWS security services?
