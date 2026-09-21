# Solution: ECS Fargate Task Definition for Payment Service

## Annotated Target

```json
{
  "family": "payment-service-prod",
  "networkMode": "awsvpc",
  "requiresCompatibilities": [
    "FARGATE"
  ],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123456789012:role/paymentServiceTaskRole",
  "containerDefinitions": [
    {
      "name": "payment-service",
      "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/payment-service:v2.1.0",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        },
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://aurora-pg.cluster-xyz.us-east-1.rds.amazonaws.com:5432/payments"
        },
        {
          "name": "SPRING_DATASOURCE_USERNAME",
          "value": "payment_master"
        },
        // Security issue: Plaintext database master password passed in container environment variables.
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "value": "SuperSecretP@ssw0rd!2026"
        },
        // Security issue: Sensitive third-party live payment API key exposed in ECS task definition metadata.
        {
          "name": "STRIPE_SECRET_KEY",
          "value": "sk_live_51NABC1234567890XYZSecretStripeKey"
        },
        // Security issue: Symmetric JWT signing key committed in plaintext, allowing unauthorized token forgery.
        {
          "name": "JWT_SIGNING_SECRET",
          "value": "f47ac10b-58cc-4372-a567-0e02b2c3d479-very-secret-jwt-key"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/payment-service-prod",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

## Discovered Issues

### Security issue: Plaintext credentials in ECS Task Definition metadata
Passing passwords, live API keys, and cryptographic signing keys inside the `environment` array exposes them through multiple attack vectors:
1. **ECS DescribeTask API**: Any IAM identity (developer, read-only auditor, CI pipeline) with `ecs:DescribeTaskDefinition` or `ecs:DescribeTasks` can read all plaintext values via the AWS CLI or AWS Console.
2. **Immutable Task Definition Revisions**: Task definitions in ECS are versioned and immutable. Every deployment leaves a permanent historical record of the password in the AWS account history, even if subsequent revisions change it.
3. **Container Inspection & Process Leaks**: In Linux containers, environment variables can be inspected by inspecting `/proc/1/environ` or executing `docker inspect` / ECS Exec. If application exceptions print environment variables or Spring Actuator `/actuator/env` is accidentally enabled, secrets leak into logs.
4. **Regulatory Violations**: Storing live payment processing keys (`STRIPE_SECRET_KEY`) in plaintext directly violates PCI-DSS Requirement 8.2 and SOC2 Trust Services Criteria.

---

## Correct Implementation

1. Store sensitive secrets in **AWS Secrets Manager** or **AWS Systems Manager (SSM) Parameter Store** (type `SecureString`) encrypted with a Customer Managed Key (CMK) in AWS KMS.
2. Reference the secret ARNs using the ECS `secrets` block instead of `environment`.
3. Grant the ECS **Task Execution Role** (`executionRoleArn`) permissions to read the secret (`secretsmanager:GetSecretValue`) and decrypt the KMS key (`kms:Decrypt`).
4. At container launch, the ECS container agent securely fetches the secret values and injects them directly into the container's environment in-memory, without exposing them in the task definition JSON.

See [`correct/task-definition.json`](correct/task-definition.json) for the production-grade ECS Task Definition.
