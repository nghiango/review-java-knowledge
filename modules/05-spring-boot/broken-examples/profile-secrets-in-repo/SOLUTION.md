# Solution: Profile Secrets Committed in Repository

## Annotated Code

### `application-prod.yml`

```yaml
payment:
  gateway:
    endpoint-url: "https://api.payments.prod.company.com/v1"
    # Security issue: Hardcoding production secret keys in VCS violates the 12-factor app rule and leaks API credentials
    # Security issue: Live credentials committed to git history cannot be erased without rotating keys and cleaning repository logs
    api-key: "live_sk_prod_998877665544332211"
spring:
  datasource:
    url: "jdbc:postgresql://db-primary.prod.internal:5432/payments"
    username: "prod_db_user"
    # Security issue: Plaintext database master password committed directly in environment profile file
    password: "SuperSecretProductionPassword2026!"
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `application-prod.yml:5` | `Security` | Hardcoded production API key | Leaks third-party payment gateway credentials to anyone with read access to the git repository. |
| `application-prod.yml:10` | `Security` | Plaintext production database password | Exposes database administrator credentials in git history. |
| `PaymentGatewayClient.java:13` | `Maintainability` | Direct property string injection | Lacks fallback environment variable binding structure and centralized validation. |

## Correct implementation

- Package: `lab.springboot.externalizedsecrets`
- Production reference: `PaymentGatewayProperties.java`, `PaymentGatewayClient.java`, `application.yml`
- Fix: Remove secrets from all committed YAML profiles. Configure property placeholders with environment variable overrides (e.g. `api-key: "${PAYMENT_GATEWAY_API_KEY:}"`) and enforce non-blank validation at application startup via `@NotBlank` on `@ConfigurationProperties`. Secrets must be supplied via environment variables, Kubernetes Secrets, or a secret manager (AWS Secrets Manager / HashiCorp Vault) at runtime.
