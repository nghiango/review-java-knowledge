# AWS Code Review Practice

Review the following production infrastructure-as-code configurations and AWS IAM policies. Identify security vulnerabilities, availability hazards, compliance violations, and disaster recovery risks before expanding the solution panels.

---

## 1. Over-Permissive ECS Task IAM Policy

An engineering team created this IAM policy for an Order Processing microservice running on AWS ECS Fargate. The service needs to upload and download invoice PDFs to S3, consume messages from an SQS order queue, and decrypt payment tokens using an AWS KMS Customer Managed Key (CMK).

### Broken Target: `policy.json`

--8<-- "modules/25-aws/broken-examples/over-permissive-iam-policy/policy.json"

??? question "Reveal issues"
    1. **Wildcard Actions (`"Action": "s3:*"`, `"sqs:*"`, `"kms:*"`)**:
       - Violates the Principle of Least Privilege.
       - `s3:*` allows deleting buckets, modifying bucket policies, and altering encryption keys.
       - `sqs:*` allows purging production queues (`sqs:PurgeQueue`) or deleting the queue entirely (`sqs:DeleteQueue`).
       - `kms:*` allows disabling customer managed keys (`kms:DisableKey`) and scheduling their permanent deletion (`kms:ScheduleKeyDeletion`).
    2. **Unbounded Resources (`"Resource": "*"`)**:
       - `"Resource": "*"` grants access across every S3 bucket, SQS queue, and KMS key in the entire AWS account. A compromise of this single container allows lateral movement across all workloads and customer data tenants.
    3. **Missing Condition Keys**:
       - Cryptographic operations like `kms:Decrypt` lack `kms:EncryptionContext` conditions, allowing any ciphertext encrypted under the CMK to be decrypted without domain isolation.

---

## 2. Plaintext Secrets in ECS Task Definition

This ECS Task Definition registers the production deployment of a payment service running on AWS Fargate. It passes database credentials, live payment gateway tokens, and cryptographic signing keys to the container.

### Broken Target: `task-definition.json`

--8<-- "modules/25-aws/broken-examples/secrets-in-environment-variables/task-definition.json"

??? question "Reveal issues"
    1. **Plaintext Secrets in `environment` Array**:
       - `SPRING_DATASOURCE_PASSWORD`, `STRIPE_SECRET_KEY`, and `JWT_SIGNING_SECRET` are passed in plaintext.
       - Any developer, read-only auditor, or CI/CD service with `ecs:DescribeTaskDefinition` or `ecs:DescribeTasks` permissions can view all plaintext secrets via the AWS CLI or AWS Console.
    2. **Immutable Task Definition Revisions Retain Secrets in History**:
       - ECS task definitions are versioned and immutable. Every deployment leaves a permanent historical record of the password in the AWS account history, even if subsequent revisions change it.
    3. **Process & Log Leaks**:
       - Environment variables can be inspected by inspecting `/proc/1/environ` or executing ECS Exec. If application exceptions print environment variables or Spring Actuator `/actuator/env` is accidentally enabled, secrets leak into CloudWatch logs.
    4. **Compliance Violations**:
       - Exposing live payment processing keys (`STRIPE_SECRET_KEY`) in plaintext directly violates PCI-DSS Requirement 8.2 and SOC2 Trust Services Criteria.

---

## 3. Single-AZ Production RDS PostgreSQL with Disabled Backups

This Terraform configuration provisions the primary relational database for an e-commerce order management service.

### Broken Target: `rds.tf`

--8<-- "modules/25-aws/broken-examples/single-az-rds-no-backup/rds.tf"

??? question "Reveal issues"
    1. **Single-AZ Deployment (`multi_az = false`)**:
       - Lacks an automated synchronous standby replica. Any underlying hardware failure, hypervisor crash, or Availability Zone network partition results in $15-35+\text{minutes}$ of complete application downtime while AWS provisions replacement hardware.
    2. **Disabled Automated Backups (`backup_retention_period = 0`)**:
       - Completely disables continuous WAL archiving to S3. Eliminates Point-In-Time-Recovery (PITR); in the event of accidental data deletion (`DROP TABLE`) or ransomware, data is permanently lost.
    3. **Public Accessibility (`publicly_accessible = true`)**:
       - Assigns a public IPv4 address directly to the database instance, exposing the PostgreSQL port (5432) to the internet and making it vulnerable to brute-force attacks and zero-day networking exploits.
    4. **Unencrypted Storage (`storage_encrypted = false`)**:
       - Leaves data blocks, transaction logs, and snapshots unencrypted on underlying EBS volumes, violating SOC2, HIPAA, and PCI-DSS compliance mandates.
    5. **Disabled Deletion Protection and Final Snapshot**:
       - `deletion_protection = false` and `skip_final_snapshot = true` allow an accidental `terraform destroy` command to wipe out the database immediately with zero recovery snapshots.
