# Solution: ECS Task IAM Policy

## Annotated Target

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowS3Access",
      "Effect": "Allow",
      // Security issue: Overly broad wildcard action "s3:*" grants administrative, bucket deletion, and public policy modification rights.
      "Action": "s3:*",
      // Security issue: Wildcard resource "*" grants read, write, and delete permissions across all S3 buckets in the entire AWS account.
      "Resource": "*"
    },
    {
      "Sid": "AllowSQSAccess",
      "Effect": "Allow",
      // Security issue: Wildcard "sqs:*" grants queue deletion (sqs:DeleteQueue), purge (sqs:PurgeQueue), and IAM policy tampering (sqs:SetQueueAttributes).
      "Action": [
        "sqs:*"
      ],
      // Security issue: Wildcard account and queue ARN grants access to all SQS queues in all regions, enabling lateral movement across workloads.
      "Resource": "arn:aws:sqs:*:*:*"
    },
    {
      "Sid": "AllowKMSOperations",
      "Effect": "Allow",
      // Security issue: Wildcard "kms:*" allows disabling keys (kms:DisableKey), scheduling key deletion (kms:ScheduleKeyDeletion), and decrypting any KMS key in the account.
      "Action": "kms:*",
      // Security issue: Wildcard resource grants access to all KMS keys, compromising cryptographic isolation between distinct microservices.
      "Resource": "*"
    }
  ]
}
```

## Discovered Issues

### Security issue: Wildcard actions violate the Principle of Least Privilege
Granting `"Action": "s3:*"`, `"sqs:*"`, and `"kms:*"` empowers the container workload with full administrative power over S3, SQS, and KMS. In the event of a container compromise (such as a Log4Shell-style RCE or malicious npm/maven dependency), an attacker can:
- Delete or purge entire production SQS queues (`sqs:PurgeQueue`, `sqs:DeleteQueue`), causing catastrophic data loss.
- Schedule deletion of KMS customer master keys (`kms:ScheduleKeyDeletion`), rendering all encrypted database snapshots, EBS volumes, and S3 objects permanently unrecoverable after the deletion window.
- Delete S3 buckets, alter bucket policies, or exfiltrate all company data stored across any bucket in the AWS account.

### Security issue: Unbounded resources (`"Resource": "*"`) eliminate blast radius containment
Specifying `"Resource": "*"` allows the ECS task to operate on every resource belonging to that service across the entire account. An order service should only touch its own dedicated invoice bucket (`arn:aws:s3:::company-order-invoices-prod/*`), its specific order queue (`arn:aws:sqs:us-east-1:123456789012:order-fulfillment-prod`), and its dedicated encryption CMK (`arn:aws:kms:us-east-1:123456789012:key/uuid`).

### Security issue: Missing cryptographic context conditions
When invoking `kms:Decrypt` and `kms:GenerateDataKey`, policies should enforce `kms:EncryptionContext` conditions ensuring that encrypted ciphertext cannot be decrypted outside the intended application domain or customer tenant context.

---

## Correct Implementation

See [`correct/policy.json`](correct/policy.json) for the production-grade least-privilege IAM policy.
