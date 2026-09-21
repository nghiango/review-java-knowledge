# AWS Architecture Solutions & Best Practices

Detailed engineering walkthroughs for correcting the broken infrastructure and security targets identified in [Code Review](code-review.md).

---

## 1. Production Least-Privilege IAM Policy

### Correct Implementation: `policy.json`

--8<-- "modules/25-aws/broken-examples/over-permissive-iam-policy/correct/policy.json"

### Architectural Rationale & Trade-offs
1. **Narrow Action Scoping**:
   - Replaced wildcard `s3:*` with specific object-level permissions: `s3:GetObject`, `s3:PutObject`, and `s3:AbortMultipartUpload`. Destructive administrative actions (`s3:DeleteBucket`, `s3:PutBucketPolicy`) are prevented.
   - Replaced wildcard `sqs:*` with consumer operations: `sqs:ReceiveMessage`, `sqs:DeleteMessage`, `sqs:GetQueueAttributes`, and `sqs:ChangeMessageVisibility`. Destructive actions (`sqs:PurgeQueue`, `sqs:DeleteQueue`) are blocked.
   - Replaced wildcard `kms:*` with cryptographic operations: `kms:Decrypt` and `kms:GenerateDataKey`. Key administrative actions (`kms:DisableKey`, `kms:ScheduleKeyDeletion`) are blocked.
2. **Resource ARN Boundaries**:
   - S3 object permissions are constrained strictly to `arn:aws:s3:::company-order-invoices-prod/invoices/*`.
   - SQS permissions are restricted strictly to the explicit queue ARN `arn:aws:sqs:us-east-1:123456789012:order-fulfillment-prod`.
   - KMS operations are locked to the specific Customer Managed Key ARN.
3. **Cryptographic Context Enforcement**:
   - The KMS statement requires `kms:EncryptionContext:service = "order-fulfillment"` and `kms:ViaService = "s3.us-east-1.amazonaws.com"`. This prevents cryptographic tokens or ciphertexts stolen from other systems from being decrypted with this key.

---

## 2. Secure Secret Injection in ECS Task Definitions

### Correct Implementation: `task-definition.json`

--8<-- "modules/25-aws/broken-examples/secrets-in-environment-variables/correct/task-definition.json"

### Architectural Rationale & Trade-offs
1. **Separation of Concerns: Task Role vs Task Execution Role**:
   - The ECS **Task Execution Role** (`executionRoleArn`) is granted `secretsmanager:GetSecretValue` and `kms:Decrypt` permissions.
   - At container bootstrap time, the ECS container agent fetches the secret value over private VPC endpoints and injects it directly into the container's environment variables in-memory.
   - The **Task Role** (`taskRoleArn`) does not need Secrets Manager read permissions, maintaining strict separation between deployment credentials and application runtime permissions.
2. **Zero Secrets in AWS Metadata**:
   - Calling `aws ecs describe-task-definition` or inspecting task revisions reveals only Secret ARNs, never plaintext passwords or payment API keys.
3. **Automated Secret Rotation Compatibility**:
   - Because the task definition points to the secret ARN (with optional JSON key extraction `:password::`), updating the secret in AWS Secrets Manager allows containers to pick up newly rotated credentials upon task restart or scheduled rolling update.

---

## 3. Resilient Multi-AZ Production RDS PostgreSQL Configuration

### Correct Implementation: `rds.tf`

--8<-- "modules/25-aws/broken-examples/single-az-rds-no-backup/correct/rds.tf"

### Architectural Rationale & Trade-offs
1. **Multi-AZ Synchronous Standby**:
   - Setting `multi_az = true` provisions a passive synchronous standby replica in a secondary Availability Zone with physical storage mirroring.
   - Guarantees $RPO = 0$ and automated DNS failover in $60-120\text{seconds}$ during host hardware crashes or AZ outages.
2. **Automated Continuous Backups & Point-In-Time-Recovery (PITR)**:
   - `backup_retention_period = 30` enables continuous automated backup of database transaction logs (WAL) to Amazon S3.
   - Supports restoring to any second within the past 30 days.
3. **Isolated Network Placement**:
   - `publicly_accessible = false` ensures the database resides strictly in private isolated subnets without public IPv4 addresses, accessible only by application security groups.
4. **Defense in Depth**:
   - `storage_encrypted = true` with a dedicated AWS KMS Customer Managed Key.
   - `deletion_protection = true` prevents accidental database destruction via Terraform or AWS CLI.
   - `skip_final_snapshot = false` guarantees a final recovery snapshot is created if the instance is ever decommissioned.
   - `enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]` streams database engine logs and slow queries directly into CloudWatch for proactive error tracking.
