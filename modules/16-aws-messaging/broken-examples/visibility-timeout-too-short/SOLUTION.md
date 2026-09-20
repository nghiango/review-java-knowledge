# Solution — SQS Visibility Timeout Shorter Than Processing Time

## Annotated code

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: Video Rendering SQS Queue and Processing Lambda

Resources:
  VideoRenderQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: video-render-queue
      # Reliability issue: SQS VisibilityTimeout (30s) is significantly shorter than the Lambda Function Timeout (90s).
      # In SQS, VisibilityTimeout acts as a temporary processing lease. If consumer execution exceeds this duration,
      # SQS automatically marks the message visible again in the queue. Another worker poll receives and processes
      # the exact same message concurrently, resulting in duplicate processing, wasted compute, and race conditions.
      # AWS best practice recommends VisibilityTimeout >= 6 * Lambda Timeout (here >= 540 seconds).
      VisibilityTimeout: 30
      MessageRetentionPeriod: 86400
      ReceiveMessageWaitTimeSeconds: 20

  VideoRenderFunction:
    Type: AWS::Lambda::Function
    Properties:
      FunctionName: video-render-processor
      Runtime: java21
      Handler: lab.awsmessaging.RenderHandler::handleRequest
      Timeout: 90
      MemorySize: 2048
      Code:
        S3Bucket: deploy-artifacts
        S3Key: video-render-processor.jar

  VideoRenderEventSourceMapping:
    Type: AWS::Lambda::EventSourceMapping
    Properties:
      EventSourceArn: !GetAtt VideoRenderQueue.Arn
      FunctionName: !GetAtt VideoRenderFunction.Arn
      # Scalability issue: BatchSize of 10 combined with heavy 45-60s tasks will exceed the 90s function timeout.
      # If 10 messages arrive in one invocation, processing them sequentially requires up to 600s, guaranteeing
      # that the Lambda function times out at 90s, rolling back processing and causing redelivery loops.
      BatchSize: 10
      Enabled: true
```

## Issue list

### Reliability issue: VisibilityTimeout shorter than consumer execution causes duplicate processing

- **Location:** `sqs-consumer-config.yaml:10`
- **Description:** SQS `VisibilityTimeout` is set to 30 seconds while the processing Lambda `Timeout` is set to 90 seconds.
- **Impact:** When a video transcoding job takes more than 30 seconds, SQS makes the in-flight message visible again to other consumers. A second Lambda instance starts processing the identical video while the first instance is still running. Both write output files to S3 and trigger downstream events, causing duplicate processing and race conditions.
- **Remediation:** Set `VisibilityTimeout` to at least 6 times the consumer Lambda timeout (`540` seconds) or implement heartbeating via `ChangeMessageVisibility` in long-running consumers.

### Scalability issue: Large batch size for long-running jobs causes batch timeout failures

- **Location:** `sqs-consumer-config.yaml:31`
- **Description:** `BatchSize: 10` for jobs taking 45–60s each guarantees timeout on the 90-second Lambda function.
- **Impact:** Even 2 jobs in a batch will take ~90–120s, causing Lambda timeout before the entire batch completes. Unfinished and finished messages alike are redelivered, generating infinite retry loops.
- **Remediation:** Set `BatchSize: 1` for heavy compute jobs, or enable `ReportBatchItemFailures` and parallelize processing within the worker.

## Correct implementation

See [`correct/sqs-consumer-config.yaml`](correct/sqs-consumer-config.yaml).

Detailed discussion in [Solutions](../../../docs/topics/aws-messaging/solutions.md#visibility-timeout-sizing-and-batch-tuning).
