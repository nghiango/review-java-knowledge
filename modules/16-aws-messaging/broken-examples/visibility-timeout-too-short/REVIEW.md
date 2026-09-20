# Code Review — SQS Visibility Timeout Shorter Than Processing Time

## Context

A video rendering pipeline offloads transcode requests to an AWS Lambda function triggered by an SQS queue. The Lambda function takes 45 to 60 seconds to process heavy video frames.

Review `sqs-consumer-config.yaml` for message lease lifecycle risks, duplicate execution vulnerabilities, and timeout configuration mismatches.

## What to look for

- Relationship between SQS `VisibilityTimeout` and consumer execution timeout
- Idempotency and duplicate delivery hazards under load
- Consumer batch size and total batch execution window
- Message deletion timing (`DeleteMessage` vs processing completion)
