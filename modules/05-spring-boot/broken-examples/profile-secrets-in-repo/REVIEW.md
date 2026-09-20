# Code Review Target: Profile Secrets Committed in Repository

Review the following production configuration profile file and payment client component. Identify security vulnerabilities and configuration management flaws.

## Files Under Review

- `application-prod.yml`
- `PaymentGatewayClient.java`

## Review Objectives

1. Identify where sensitive credentials and API keys are stored.
2. Evaluate what happens when repository access is granted to developers or third-party tooling.
3. Determine how secrets should be injected at runtime across environments without committing them to version control.
