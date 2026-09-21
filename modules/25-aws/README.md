# Module 25 — AWS

This is a **doc module** covering cloud architecture and AWS engineering fundamentals for Senior Java / Spring Boot Backend Engineers:
IAM least-privilege policies and role assumption (IRSA / ECS Task Roles), secure credential handling via AWS Secrets Manager and SSM Parameter Store with customer-managed KMS keys, Multi-AZ high availability and automated failover for Amazon RDS PostgreSQL, container orchestration tradeoffs across ECS Fargate and EKS, serverless computing with AWS Lambda (cold starts, SnapStart, provisioned concurrency), messaging integration with SQS and SNS (FIFO vs standard, DLQ, visibility timeouts), event-driven architecture via EventBridge, resilient traffic routing with ALB and NLB, and multi-region failover.

The canonical prose, architecture blueprints, interview Q&A, and operational incident guides live in the documentation:

👉 **[AWS Documentation](../../docs/topics/aws/index.md)**

## Broken Review Examples

This module provides 3 realistic broken review targets under `broken-examples/`:

1. `over-permissive-iam-policy/` — IAM policy granting wildcard actions (`"Action": "*"`) and resources (`"Resource": "*"`) to an ECS task role, lacking condition keys and principle of least privilege, exposing S3 data and KMS master keys to lateral movement.
2. `secrets-in-environment-variables/` — ECS Task Definition passing raw database passwords and third-party API tokens directly in plaintext container `environment` blocks, exposing secrets to ECS DescribeTask API callers, CloudWatch logs, and container process inspection.
3. `single-az-rds-no-backup/` — Terraform RDS PostgreSQL resource configured as a single-AZ instance (`multi_az = false`), with disabled automated backups (`backup_retention_period = 0`), disabled storage encryption (`storage_encrypted = false`), and enabled public accessibility (`publicly_accessible = true`).
