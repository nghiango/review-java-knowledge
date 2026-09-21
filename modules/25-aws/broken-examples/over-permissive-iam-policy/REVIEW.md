# Code Review: ECS Task IAM Policy

## Pull Request Description
This PR introduces the IAM policy for our new Order Fulfillment Service deployed on AWS ECS Fargate. The service needs to:
1. Upload and download generated customer invoice PDFs to and from our designated S3 bucket.
2. Poll incoming order events from the production SQS queue and delete processed messages.
3. Encrypt and decrypt sensitive customer payment metadata using our AWS KMS Customer Managed Key (CMK).

The author states this policy was tested in staging and confirmed that all S3 uploads, SQS polling, and KMS decryption calls work without `AccessDeniedException`.

## Files Under Review
- `policy.json` — IAM execution role policy attached to the ECS Task Role.

## Review Questions
1. Does this IAM policy adhere to the AWS Well-Architected Framework and Principle of Least Privilege?
2. What are the security and operational blast radiuses if this container is compromised via Remote Code Execution (RCE) or a dependency CVE?
3. Which actions and resource ARNs should be explicitly narrowed down?
