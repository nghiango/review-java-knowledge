# AWS Messaging Exercises

Hands-on architectural and infrastructure-as-code exercises covering EventBridge pattern filtering, SQS dead-letter policies, and resilient worker lease extension.

---

## Exercise 1: EventBridge Content-Based Routing with SQS Dead-Letter Fallback

### Problem Statement

Design a serverless order routing architecture using Amazon EventBridge. A central event bus receives order lifecycle events from microservices. Route events matching the following conditions to a dedicated `PriorityOrderQueue`:
1. `source`: `ecommerce.orders`
2. `detail-type`: `OrderPlaced` or `OrderUpgraded`
3. `detail.totalAmount`: $> \$1000$ OR `detail.customer.tier`: `VIP` or `ENTERPRISE`

Any events that fail delivery from EventBridge to SQS must be routed to an `EventBridgeDeadLetterQueue`.

### Requirements
- Define an EventBridge Event Bus `EnterpriseEventBus`.
- Define an EventBridge Rule `PriorityOrderRule` with JSON pattern matching.
- Define a Target pointing to `PriorityOrderQueue` with an attached dead-letter SQS queue.

??? question "Reveal solution"
    ```yaml
    AWSTemplateFormatVersion: '2010-09-09'
    Description: EventBridge Content-Based Routing with Dead-Letter SQS Queue

    Resources:
      EnterpriseEventBus:
        Type: AWS::Events::EventBus
        Properties:
          Name: enterprise-orders-bus

      PriorityOrderDeadLetterQueue:
        Type: AWS::SQS::Queue
        Properties:
          QueueName: priority-order-dlq

      PriorityOrderQueue:
        Type: AWS::SQS::Queue
        Properties:
          QueueName: priority-order-queue
          VisibilityTimeout: 120
          ReceiveMessageWaitTimeSeconds: 20

      PriorityOrderRule:
        Type: AWS::Events::Rule
        Properties:
          Name: priority-order-rule
          EventBusName: !GetAtt EnterpriseEventBus.Arn
          EventPattern:
            source:
              - ecommerce.orders
            detail-type:
              - OrderPlaced
              - OrderUpgraded
            detail:
              $or:
                - totalAmount:
                    - numeric:
                        - '>'
                        - 1000
                - customer:
                    tier:
                      - VIP
                      - ENTERPRISE
          Targets:
            - Id: PriorityOrderQueueTarget
              Arn: !GetAtt PriorityOrderQueue.Arn
              DeadLetterConfig:
                Arn: !GetAtt PriorityOrderDeadLetterQueue.Arn
    ```

---

## Exercise 2: Resilient SQS Worker with Dynamic Visibility Heartbeat

### Problem Statement

Design a resilient worker execution loop using the AWS SDK for Java v2. The worker pulls tasks from an SQS queue where processing time ranges from 15 seconds to 10 minutes. 

To prevent duplicate execution while avoiding an excessively high static visibility timeout:
1. Initialize task visibility to 60 seconds.
2. Spawn a background heartbeat task that calls `ChangeMessageVisibility` every 20 seconds, extending the lease by 60 seconds as long as processing is active.
3. If processing finishes, cancel the heartbeat, invoke `DeleteMessage`, and log completion.
4. If the processing thread throws an exception, cancel the heartbeat and immediately set visibility to 0 (`ChangeMessageVisibility(0)`) so the message is retried without waiting for the lease to expire.

??? question "Reveal solution"
    ```java
    // Production worker lease extension pattern with AWS SDK v2
    class ResilientQueueWorker {

        private final SqsClient sqsClient;
        private final String queueUrl;
        private final ScheduledExecutorService heartbeatScheduler;

        public ResilientQueueWorker(SqsClient sqsClient, String queueUrl) {
            this.sqsClient = sqsClient;
            this.queueUrl = queueUrl;
            this.heartbeatScheduler = Executors.newScheduledThreadPool(2);
        }

        public void processWithHeartbeat(Message message, Consumer<Message> taskProcessor) {
            String receiptHandle = message.receiptHandle();

            // Schedule recurring heartbeat every 20 seconds to extend visibility
            ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
                try {
                    sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(receiptHandle)
                            .visibilityTimeout(60) // Extend by 60s
                            .build());
                } catch (Exception ex) {
                    System.err.println("Failed to extend visibility: " + ex.getMessage());
                }
            }, 20, 20, TimeUnit.SECONDS);

            try {
                // Execute long-running business logic
                taskProcessor.accept(message);

                // Successfully finished: delete message from queue
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(receiptHandle)
                        .build());
            } catch (Exception ex) {
                // Processing failed: release message immediately back to queue
                try {
                    sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(receiptHandle)
                            .visibilityTimeout(0) // Return to queue immediately
                            .build());
                } catch (Exception releaseEx) {
                    System.err.println("Failed to release message: " + releaseEx.getMessage());
                }
                throw ex;
            } finally {
                // Always cancel background heartbeat lease extension
                heartbeatTask.cancel(true);
            }
        }
    }
    ```

---

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Production](production.md)
