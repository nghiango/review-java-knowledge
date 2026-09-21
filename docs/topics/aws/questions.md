# AWS Interview Questions

Senior Java and backend engineer interview questions covering AWS architecture, IAM security, database resilience, compute tradeoffs, messaging, and operational incident troubleshooting.

---

<!-- --8<-- [start:basic] -->
## Basic Concepts (1–8)

### 1. What are the key architectural and operational differences between Amazon EC2, Amazon ECS, and AWS Lambda for running Java backend workloads?

What factors determine when to deploy a Spring Boot service to EC2, ECS (Fargate), or AWS Lambda?

??? question "Reveal answer"
    - **Amazon EC2 (Virtual Machines)**:
      - *Architecture*: Full control over operating system, kernel parameters, JVM installation, and local storage.
      - *Operational Tradeoff*: High operational overhead. You are responsible for OS security patching, AMI baking, autoscaling group tuning, and capacity management. Best suited for legacy monolithic applications with custom C-libraries, stateful services, or specialized kernel requirements.
    - **Amazon ECS / EKS (Containers)**:
      - *Architecture*: Containerized Spring Boot applications running on Docker. With AWS Fargate, AWS provisions and manages the underlying compute nodes per task.
      - *Operational Tradeoff*: Moderate operational overhead. Predictable steady-state memory and CPU utilization, multi-threaded request processing, warm JVM performance without cold-start jitter, and native container tooling. The industry standard for enterprise microservices.
    - **AWS Lambda (Serverless Functions)**:
      - *Architecture*: Event-driven compute where AWS provisions execution environments on-demand. Billed per millisecond of execution.
      - *Operational Tradeoff*: Minimal operational overhead (no server management). However, traditional Java runtimes suffer from cold starts ($2-8\text{s}$). Concurrency model executes one request per environment instance at a time. Ideal for bursty, asynchronous event handlers (S3 file triggers, SQS batch workers, scheduled cron tasks) or HTTP APIs leveraging AWS Lambda SnapStart.

??? example "Example"
    ```java
    // AWS Lambda Handler processing incoming SQS events with minimal cold start overhead
    public class OrderSqsHandler implements RequestHandler<SQSEvent, Void> {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public Void handleRequest(SQSEvent event, Context context) {
            for (SQSEvent.SQSMessage msg : event.getRecords()) {
                context.getLogger().log("Processing message ID: " + msg.getMessageId());
                // Process order idempotently...
            }
            return null;
        }
    }
    ```

---

### 2. What is the difference between IAM Identity-Based Policies and Resource-Based Policies?

How do they evaluate access when an IAM role in Account A attempts to read an S3 bucket in Account B?

??? question "Reveal answer"
    - **Identity-Based Policies**:
      - Attached directly to IAM Users, Groups, or Roles.
      - Specify what actions the principal can perform and on which AWS resource ARNs (e.g., granting an ECS Task Role permission to read SQS).
    - **Resource-Based Policies**:
      - Attached directly to AWS resources (e.g., S3 Bucket Policies, SQS Queue Policies, KMS Key Policies, Secrets Manager Secret Policies).
      - Specify *who* (the `Principal` element) can perform actions on that specific resource and under what `Condition` blocks.
    - **Cross-Account Evaluation Rule**:
      - For same-account access: access is granted if **either** the identity policy OR the resource policy allows it (and neither has an explicit deny).
      - For cross-account access (Account A role reading Account B S3 bucket): access is granted **only if both** the identity policy in Account A allows the action AND the resource policy in Account B explicitly allows the Account A principal.

??? example "Example"
    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Sid": "CrossAccountBucketAccess",
          "Effect": "Allow",
          "Principal": {
            "AWS": "arn:aws:iam::111122223333:role/order-service-task-role"
          },
          "Action": "s3:GetObject",
          "Resource": "arn:aws:s3:::central-invoices-bucket/orders/*"
        }
      ]
    }
    ```

---

### 3. How does Amazon S3 consistency work, and when should you use S3 Bucket Policies versus IAM Policies?

What consistency guarantees does S3 provide for PUT, DELETE, and GET operations?

??? question "Reveal answer"
    - **S3 Consistency Model**:
      - Amazon S3 delivers **strong read-after-write consistency** for `PUT` and `DELETE` requests of objects in all AWS regions.
      - After a successful write of a new object or overwrite/deletion of an existing object, any subsequent `GET` or `LIST` operation immediately returns the latest mutation.
      - Concurrent writes to the same key resolve via last-writer-wins based on timestamp.
    - **S3 Bucket Policies vs IAM Policies**:
      - Use **IAM Policies** when managing permissions centralized around specific roles or applications (e.g., "Allow the Payment Service role to write invoices").
      - Use **S3 Bucket Policies** when enforcing rules across all callers of a bucket regardless of identity (e.g., enforcing TLS encryption in transit via `aws:SecureTransport: false` deny, denying unencrypted object uploads, or granting cross-account read permissions).

??? example "Example"
    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Sid": "EnforceTLSRequestsOnly",
          "Effect": "Deny",
          "Principal": "*",
          "Action": "s3:*",
          "Resource": [
            "arn:aws:s3:::production-financial-records",
            "arn:aws:s3:::production-financial-records/*"
          ],
          "Condition": {
            "Bool": {
              "aws:SecureTransport": "false"
            }
          }
        }
      ]
    }
    ```

---

### 4. What is the fundamental operational difference between an Amazon RDS Multi-AZ deployment and an Amazon RDS Read Replica?

Can you execute read queries against a Multi-AZ standby replica?

??? question "Reveal answer"
    - **Amazon RDS Multi-AZ**:
      - *Purpose*: High availability and automated disaster recovery.
      - *Replication*: Synchronous block-level physical storage replication across two Availability Zones within the same region.
      - *Query Capability*: The standby instance is passive and **cannot accept read or write queries**.
      - *Failover*: Automated failover in $60-120\text{seconds}$ via DNS CNAME flip with zero data loss ($RPO = 0$).
    - **Amazon RDS Read Replica**:
      - *Purpose*: Horizontal read scalability and analytical query offloading.
      - *Replication*: Asynchronous streaming logical/database-level replication from the primary.
      - *Query Capability*: Active read-only database instance that accepts read queries.
      - *Failover*: Manual promotion to standalone primary (or via automated scripts). Incurs asynchronous replication lag, meaning promotion may suffer data loss ($RPO > 0$).

??? example "Example"
    ```java
    // RoutingDataSource separating transactional writes to Primary from read queries to Read Replica
    public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            return isReadOnly ? DataSourceType.READ_REPLICA : DataSourceType.PRIMARY;
        }
    }
    ```

---

### 5. What are the differences between Amazon SQS Standard Queues and Amazon SQS FIFO Queues?

What are the throughput limits and ordering guarantees of each?

??? question "Reveal answer"
    - **Amazon SQS Standard Queues**:
      - *Throughput*: Nearly unlimited API transactions per second.
      - *Delivery*: **At-least-once delivery** (occasional duplicate messages may be delivered).
      - *Ordering*: **Best-effort ordering**; messages may be consumed out of publication order.
      - *Use Case*: High-throughput decoupled workloads where consumers are strictly idempotent and message order does not impact correctness.
    - **Amazon SQS FIFO (First-In-First-Out) Queues**:
      - *Throughput*: Up to 3,000 messages/sec with batching (300 msgs/sec without batching) or high-throughput mode up to 70,000 msgs/sec.
      - *Delivery*: **Exactly-once processing** via 5-minute deduplication intervals (`MessageDeduplicationId`).
      - *Ordering*: Strict FIFO ordering guaranteed per `MessageGroupId`.
      - *Use Case*: Financial ledgers, stock trade execution, inventory updates where ordering is critical.

??? example "Example"
    ```java
    // Publishing to an SQS FIFO Queue requires MessageGroupId and optional MessageDeduplicationId
    SendMessageRequest request = SendMessageRequest.builder()
        .queueUrl("https://sqs.us-east-1.amazonaws.com/123456789012/orders.fifo")
        .messageBody("{\"orderId\":\"ORD-1001\",\"status\":\"PENDING\"}")
        .messageGroupId("CUSTOMER-9982") // Preserves ordering per customer
        .messageDeduplicationId("ORD-1001-v1")
        .build();
    sqsClient.sendMessage(request);
    ```

---

### 6. Explain the networking architecture of an Amazon VPC: Public Subnets, Private Subnets, Route Tables, and NAT Gateways.

Why should Spring Boot microservices never be placed in a Public Subnet?

??? question "Reveal answer"
    - **Public Subnets**:
      - Associated with a Route Table containing a route directing `0.0.0.0/0` to an **Internet Gateway (IGW)**.
      - Resources in public subnets (such as ALBs and NAT Gateways) receive public IP addresses and can receive inbound internet traffic.
    - **Private Subnets**:
      - Route Table directs `0.0.0.0/0` to a **NAT Gateway** located in a public subnet.
      - Resources (ECS containers, EKS worker nodes) possess only private IP addresses (`10.0.x.x`). They can initiate outbound connections to the internet (e.g., calling Stripe or OpenAI) but cannot be directly addressed or attacked from the public internet.
    - **Why Backend Services Belong in Private Subnets**:
      - Placing application containers in public subnets exposes their host ports directly to automated internet port scanners, DDoS attacks, and zero-day vulnerabilities.
      - Private subnets guarantee that all inbound HTTP traffic must pass through the Application Load Balancer and AWS WAF inspection.

??? example "Example"
    ```hcl
    # Terraform route table for private subnets directing outbound internet through NAT Gateway
    resource "aws_route_table" "private" {
      vpc_id = aws_vpc.main.id

      route {
        cidr_block     = "0.0.0.0/0"
        nat_gateway_id = aws_nat_gateway.nat_az_a.id
      }

      tags = { Name = "private-subnet-route-table" }
    }
    ```

---

### 7. How do CloudWatch Logs, CloudWatch Metrics, and CloudWatch Alarms integrate with Spring Boot applications?

How does high cardinality impact CloudWatch custom metric billing?

??? question "Reveal answer"
    - **CloudWatch Logs**:
      - Collects application logs stream-by-stream (e.g., via the ECS `awslogs` log driver or OpenTelemetry collector).
      - Supports Metric Filters to extract error rates (e.g., regex matching `ERROR [org.springframework]`) into CloudWatch Metrics.
    - **CloudWatch Metrics & Dimensions**:
      - Time-series data points categorized by Metric Name, Namespace, and up to 30 Dimensions (key-value tags).
      - **Cardinality Warning**: AWS CloudWatch charges \$0.30 per custom metric per month. Creating metric dimensions with high cardinality (such as `userId`, `orderId`, or `ipAddress`) spawns millions of unique metric combinations, resulting in tens of thousands of dollars in unexpected CloudWatch billing charges. Dimensions must be restricted to low-cardinality values (e.g., `HttpStatus`, `EndpointName`).
    - **CloudWatch Alarms**:
      - Monitor metric thresholds (e.g., p99 latency $> 1000\text{ms}$ or HTTP 5xx rate $> 1\%$) over evaluation periods (e.g., 3 consecutive periods of 1 minute) to trigger SNS alerts, PagerDuty incidents, or ECS autoscaling policies.

??? example "Example"
    ```json
    {
      "MetricName": "HttpResponseTime",
      "Namespace": "CustomOrderService",
      "Dimensions": [
        { "Name": "Endpoint", "Value": "POST /api/orders" },
        { "Name": "Status", "Value": "200" }
      ],
      "Value": 45.2,
      "Unit": "Milliseconds"
    }
    ```

---

### 8. What are the primary differences between an Application Load Balancer (ALB) and a Network Load Balancer (NLB)?

When would you choose an NLB over an ALB for a backend Java service?

??? question "Reveal answer"
    - **Application Load Balancer (Layer 7)**:
      - Operates at the Application Layer (HTTP/HTTPS, HTTP/2, gRPC).
      - Can inspect HTTP URLs, paths (`/api/orders`), HTTP headers, and query strings to route to different target groups.
      - Terminates TLS, injects `X-Forwarded-For` and `X-Amzn-Trace-Id` headers, and natively integrates with AWS WAF and OpenID Connect / Cognito authentication.
    - **Network Load Balancer (Layer 4)**:
      - Operates at the Transport Layer (TCP, UDP, TLS passthrough).
      - Delivers ultra-low latency (sub-millisecond) and scales instantly to tens of millions of connections without pre-warming.
      - Provides a **static Elastic IP address per Availability Zone**, enabling client IP whitelisting in firewalls.
      - Preserves client source IP address transparently at the TCP level.
    - **When to Choose NLB**:
      - Non-HTTP protocols (e.g., raw TCP socket servers, Kafka proxy, custom binary protocols).
      - Workloads requiring static public IPs for client firewall whitelisting.
      - Extreme burst traffic (e.g., instant flash sales scaling from 0 to 1,000,000 requests/sec in seconds where ALB autoscaling might lag).

??? example "Example"
    ```hcl
    # AWS ALB listener rule performing path-based routing to distinct ECS target groups
    resource "aws_lb_listener_rule" "orders_rule" {
      listener_arn = aws_lb_listener.https.arn
      priority     = 100

      action {
        type             = "forward"
        target_group_arn = aws_lb_target_group.orders_app.arn
      }

      condition {
        path_pattern {
          values = ["/api/v1/orders*"]
        }
      }
    }
    ```
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Production Architecture (9–16)

### 9. What is the difference between an ECS Task Role and an ECS Task Execution Role?

If your Spring Boot application cannot decrypt an AWS KMS key at runtime, which role is misconfigured?

??? question "Reveal answer"
    - **ECS Task Execution Role (`executionRoleArn`)**:
      - Assumed by the **AWS ECS infrastructure agent** before your application code boots up.
      - Requires permissions to: pull container images from private Amazon ECR registries, stream stdout/stderr logs to CloudWatch Logs (`awslogs-group`), and fetch encrypted secrets from AWS Secrets Manager or SSM Parameter Store to inject as environment variables into the container.
    - **ECS Task Role (`taskRoleArn`)**:
      - Assumed by the **Java / Spring Boot application process** running inside the container via the AWS SDK v2.
      - Requires domain permissions to access AWS services during runtime: writing to S3 buckets, publishing to SQS, querying DynamoDB, or calling `kms:Decrypt` via the AWS SDK.
    - **Diagnostic Answer**:
      - If the container fails to start with an ECS agent error "CannotPullContainerError" or "Fetching secret data failed", the **Task Execution Role** is misconfigured.
      - If the Spring Boot container boots up but throws `AccessDeniedException: User ... is not authorized to perform: kms:Decrypt` during application business logic execution, the **Task Role** is misconfigured.

??? example "Example"
    ```json
    {
      "family": "order-service",
      "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
      "taskRoleArn": "arn:aws:iam::123456789012:role/orderServiceAppTaskRole",
      "containerDefinitions": [
        {
          "name": "app",
          "image": "123456789012.dkr.ecr.us-east-1.amazonaws.com/order-service:1.0.0"
        }
      ]
    }
    ```

---

### 10. How does AWS Lambda SnapStart eliminate cold start latency for Java microservices, and what concurrency or state pitfalls must engineers avoid?

Why does `SecureRandom` require special attention when using SnapStart?

??? question "Reveal answer"
    - **AWS Lambda SnapStart Mechanics**:
      - When publishing a function version, AWS Lambda initializes the execution environment, loads classes, executes static initializers, runs Spring Boot context initialization, and executes JIT compilation.
      - Lambda takes a memory and disk snapshot using Firecracker microVM and caches it.
      - On subsequent function invocations, Lambda restores the execution environment from the snapshot in $< 200\text{ms}$ (eliminating classloading and Spring startup overhead).
    - **State & Randomness Pitfalls**:
      - **Snapshot Replay / Identical Random Seed**: If `java.security.SecureRandom` is initialized during application startup before the checkpoint, every restored execution environment will restore the *exact same internal PRNG seed state*. This causes distinct Lambda instances to generate identical UUIDs, duplicate cryptographic session keys, or predictable tokens.
      - **Stale Sockets & Connections**: Any database connection (HikariCP) or TCP socket opened before checkpoint will be invalid/stale upon restore.
    - **Senior Mitigation**:
      - Implement CRaC (Coordinated Restore at Checkpoint) lifecycle hooks (`org.crac.Resource`):
        - In `beforeCheckpoint`: close database connections, flush buffers.
        - In `afterRestore`: re-seed `SecureRandom`, reopen connection pools.

??? example "Example"
    ```java
    import org.crac.Core;
    import org.crac.Resource;

    public class DatabaseResource implements Resource {
        public DatabaseResource() {
            Core.getGlobalContext().register(this);
        }

        @Override
        public void beforeCheckpoint(org.crac.Context<? extends Resource> context) {
            // Close Hikari connection pool before snapshot
        }

        @Override
        public void afterRestore(org.crac.Context<? extends Resource> context) {
            // Re-initialize pool and reseed SecureRandom after restore
        }
    }
    ```

---

### 11. When should you use AWS Secrets Manager versus AWS Systems Manager (SSM) Parameter Store for Spring Boot configuration?

What are the cost and operational differences, particularly regarding automated rotation?

??? question "Reveal answer"
    - **AWS Systems Manager (SSM) Parameter Store**:
      - *Storage*: Supports `String`, `StringList`, and encrypted `SecureString` (using KMS).
      - *Cost*: Standard parameters are **completely free** (up to 10,000 parameters per account; advanced parameters cost \$0.05/month). API calls have no per-request charge under standard limits.
      - *Rotation*: Does not provide native out-of-the-box automated password rotation.
      - *Best For*: Application configuration properties, feature flags, API endpoints, and static tokens.
    - **AWS Secrets Manager**:
      - *Storage*: JSON key-value secrets encrypted with AWS KMS.
      - *Cost*: **\$0.40 per secret per month** + \$0.05 per 10,000 API calls.
      - *Rotation*: Out-of-the-box **automated rotation** using Lambda templates for Amazon RDS, Aurora, and Redshift. Automatically rotates passwords and synchronizes with the database without application downtime.
      - *Best For*: Database master credentials, payment gateway keys, and high-security compliance tokens requiring automated 30/60/90-day rotation.

??? example "Example"
    ```yaml
    # Spring Cloud AWS reading secrets directly into Spring Environment
    spring:
      config:
        import:
          - "aws-secretsmanager:/prod/order-service/database-secrets"
          - "aws-parameterstore:/prod/order-service/config/"
    ```

---

### 12. How do SQS Visibility Timeout, Long Polling (`WaitTimeSeconds`), and Dead Letter Queues (DLQs) interact in high-throughput consumers?

What happens if a worker takes 45 seconds to process a message when Visibility Timeout is set to 30 seconds?

??? question "Reveal answer"
    - **The 45s vs 30s Race Condition**:
      - If worker A receives a message at $t=0$, SQS hides the message for 30s.
      - At $t=30\text{s}$, worker A is still processing. SQS considers the message unprocessed and makes it visible again.
      - At $t=30.1\text{s}$, worker B polls SQS, receives the exact same message, and begins processing.
      - At $t=45\text{s}$, worker A finishes and calls `DeleteMessage(receiptHandle)`. If worker B has already refreshed the receipt handle or deleted it, worker A may fail, or both workers execute duplicate side-effects (e.g., charging a credit card twice).
    - **Senior Solutions**:
      1. Set Visibility Timeout greater than the maximum conceivable processing timeout ($2\times - 3\times$ p99 processing duration).
      2. If processing is unpredictable, the worker must run a background heartbeat thread calling `ChangeMessageVisibility` to extend visibility while the job is active.
      3. Design downstream consumers to be strictly idempotent using idempotency keys.
    - **Long Polling (`WaitTimeSeconds = 20`)**:
      - Eliminates empty polling responses, reduces API cost by $> 80\%$, and decreases message delivery latency to consumers.

??? example "Example"
    ```java
    // Background heartbeat task extending message visibility for long-running processing
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(() -> {
        sqsClient.changeMessageVisibility(ChangeMessageVisibilityRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(receiptHandle)
            .visibilityTimeout(30)
            .build());
    }, 15, 15, TimeUnit.SECONDS);
    ```

---

### 13. What makes Amazon Aurora's storage architecture fundamentally different from standard Amazon RDS PostgreSQL?

Why does Aurora provide faster crash recovery and negligible read replica replication lag?

??? question "Reveal answer"
    - **Decoupled Distributed Log-Structured Storage**:
      - In standard RDS, database compute instances manage their own local EBS volumes.
      - In Aurora, compute is decoupled from a purpose-built, log-structured distributed storage fleet. Storage is stripped across 3 Availability Zones, maintaining **6 copies of data blocks** (2 per AZ).
    - **"The Log Is The Database"**:
      - When committing a transaction, Aurora compute nodes only write **redo log records** over the network to the 6 storage nodes. The storage nodes asynchronously apply the log records to data pages in parallel.
      - Requires only a write quorum of 4 out of 6 nodes to confirm a commit.
    - **Crash Recovery**:
      - Standard PostgreSQL must replay WAL logs from the last checkpoint upon restart (taking minutes to hours).
      - Aurora storage nodes process redo logs continuously in the background. When an Aurora compute instance crashes and restarts, it starts in $< 30\text{seconds}$ without replaying WAL.
    - **Read Replica Lag**:
      - Aurora read replicas share the same underlying distributed storage fleet. They do not duplicate disk writes; they only receive redo log streams in-memory to update buffer pools ($< 20\text{ms}$ lag).

??? example "Example"
    ```text
    Aurora Storage Architecture (Quorum: 4/6 Write, 3/6 Read):
    [ Compute Node (Writer) ]
              │
      ┌───────┼───────┐ (Redo Log Records Only)
      ▼       ▼       ▼
    [AZ-a]  [AZ-b]  [AZ-c]
    (2x)    (2x)    (2x)   <-- 6 storage nodes total across 3 AZs
    ```

---

### 14. What is the difference between an Amazon VPC Gateway Endpoint and an Interface Endpoint (AWS PrivateLink)?

Which AWS services use Gateway Endpoints, and what are the cost implications?

??? question "Reveal answer"
    - **VPC Gateway Endpoints**:
      - *Supported Services*: Available exclusively for **Amazon S3** and **Amazon DynamoDB**.
      - *Implementation*: Configured directly in VPC Route Tables as a prefix list target (`pl-xxxx`). Traffic destined for S3 or DynamoDB is routed directly across the AWS private network backbone.
      - *Cost*: **100% Free**. Zero hourly charges and zero data processing charges.
    - **VPC Interface Endpoints (AWS PrivateLink)**:
      - *Supported Services*: Available for almost all AWS services (Secrets Manager, SQS, SNS, KMS, CloudWatch, ECR) and third-party SaaS endpoints.
      - *Implementation*: Provisions an Elastic Network Interface (ENI) with a private IP address (`10.0.x.x`) inside your private subnet. Application DNS resolves to these private IP addresses.
      - *Cost*: Charges **\$0.01 per AZ per hour** (~ \$7.20/month per AZ) + **\$0.01 per GB** data processed.
    - **Architectural Rule**: Always configure Gateway Endpoints for S3 and DynamoDB to bypass NAT Gateway data processing fees (\$0.045/GB) and Interface Endpoint costs.

??? example "Example"
    ```hcl
    # Free VPC Gateway Endpoint for Amazon S3
    resource "aws_vpc_endpoint" "s3_gateway" {
      vpc_id            = aws_vpc.main.id
      service_name      = "com.amazonaws.us-east-1.s3"
      vpc_endpoint_type = "Gateway"
      route_table_ids   = [aws_route_table.private.id]
    }
    ```

---

### 15. How does the SNS-to-SQS Fanout pattern work, and how do you implement message filtering without code changes?

What happens to other subscribers if one subscriber queue fills up or crashes?

??? question "Reveal answer"
    - **Fanout Architecture**:
      - A publisher service (e.g., Order Service) publishes an event once to an Amazon SNS topic.
      - Multiple Amazon SQS queues (e.g., Billing Service Queue, Inventory Service Queue, Analytics Queue) subscribe to that single SNS topic.
      - SNS automatically replicates and delivers the message to all subscribed SQS queues in parallel.
    - **Failure Isolation**:
      - If the Inventory Service crashes or its SQS queue fills up, messages accumulate safely in the Inventory SQS queue.
      - The Billing Service and Analytics Service are completely isolated and continue consuming events normally without latency degradation.
    - **Subscription Filter Policies**:
      - SNS allows setting JSON filter policies on each subscription evaluating `MessageAttributes`.
      - For example, the European Fulfillment SQS queue can define a filter `{"region": ["EU"]}` so it only receives European orders, while the US queue receives US orders—without any routing logic in the publisher application.

??? example "Example"
    ```json
    {
      "region": [
        "EU",
        "UK"
      ],
      "order_amount": [
        { "numeric": [ ">=", 100 ] }
      ]
    }
    ```

---

### 16. How does Envelope Encryption work with AWS KMS, and why do backend applications not send bulk data directly to KMS for encryption?

What are the performance and payload constraints of the `kms:Encrypt` API?

??? question "Reveal answer"
    - **Why Bulk Data Is Never Sent to KMS**:
      - The `kms:Encrypt` API has a hard maximum payload limit of **4 KB**.
      - Furthermore, KMS API calls are subject to account rate limits (e.g., 10,000 req/sec) and incur network latency and per-request API costs.
    - **Envelope Encryption Protocol**:
      1. To encrypt data (such as a 10MB customer file or database record), the backend calls `kms:GenerateDataKey(KeyId=cmkArn, KeySpec=AES_256)`.
      2. KMS returns:
         - A **Plaintext Data Key (DEK)** (256-bit raw bytes).
         - An **Encrypted Data Key (CiphertextBlob)** (the DEK encrypted under the Customer Managed Key).
      3. The Java application uses standard local cryptography (e.g., AES-GCM) with the plaintext DEK to encrypt the 10MB payload in memory.
      4. The application **immediately wipes the plaintext DEK from memory** and stores the encrypted payload alongside the encrypted DEK.
      5. To decrypt: the application passes the encrypted DEK to `kms:Decrypt`, receives the plaintext DEK, decrypts the payload locally, and clears the key.

??? example "Example"
    ```java
    // AWS SDK v2 Envelope Encryption snippet
    GenerateDataKeyResponse dataKey = kmsClient.generateDataKey(
        GenerateDataKeyRequest.builder()
            .keyId("arn:aws:kms:us-east-1:123456789012:key/my-cmk-id")
            .keySpec(DataKeySpec.AES_256)
            .build()
    );
    byte[] plaintextKey = dataKey.plaintext().asByteArray();
    byte[] encryptedKey = dataKey.ciphertextBlob().asByteArray();
    // Encrypt payload locally using plaintextKey with AES-GCM...
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Production Engineering (17–21)

### 17. How does IAM Roles for Service Accounts (IRSA) work internally on Amazon EKS, and how does it prevent pod-to-node privilege escalation?

Trace the authentication handshake between a Spring Boot pod, Kubernetes, and AWS STS.

??? question "Reveal answer"
    - **Why Traditional Node Roles Are Dangerous**:
      - Without IRSA, pods running on an EKS worker node inherit the EC2 instance profile of that node. If a single compromised pod has access to the EC2 metadata service (`169.254.169.254`), it gains access to the IAM permissions of every other pod on that node.
    - **IRSA Handshake Protocol**:
      1. An IAM OIDC identity provider is established for the EKS cluster.
      2. A Kubernetes `ServiceAccount` is created with annotation `eks.amazonaws.com/role-arn: arn:aws:iam::...:role/payment-role`.
      3. An IAM Role is created with a Trust Relationship restricting the `Federated` principal to the EKS OIDC provider and the `sub` claim to `system:serviceaccount:default:payment-sa`.
      4. When the pod starts, the EKS Pod Identity Webhook mounts a projected OIDC token (JWT signed by Kubernetes private keys) at `/var/run/secrets/eks.amazonaws.com/serviceaccount/token` and sets `AWS_WEB_IDENTITY_TOKEN_FILE`.
      5. The AWS SDK v2 calls `sts:AssumeRoleWithWebIdentity` passing the token.
      6. AWS STS validates the token's cryptographic signature against the EKS cluster's public keys hosted at its OIDC endpoint and returns temporary scoped AWS credentials.

??? example "Example"
    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Effect": "Allow",
          "Principal": {
            "Federated": "arn:aws:iam::123456789012:oidc-provider/oidc.eks.us-east-1.amazonaws.com/id/EXAMPLE"
          },
          "Action": "sts:AssumeRoleWithWebIdentity",
          "Condition": {
            "StringEquals": {
              "oidc.eks.us-east-1.amazonaws.com/id/EXAMPLE:sub": "system:serviceaccount:production:order-service-sa",
              "oidc.eks.us-east-1.amazonaws.com/id/EXAMPLE:aud": "sts.amazonaws.com"
            }
          }
        }
      ]
    }
    ```

---

### 18. How should Amazon RDS Proxy be sized and configured when connecting from Spring Boot applications running HikariCP?

Why does setting `maximumPoolSize = 50` per replica across 20 autoscaled ECS tasks create a connection crisis without RDS Proxy?

??? question "Reveal answer"
    - **The Mathematical Crisis**:
      - 20 autoscaled ECS tasks with `maximumPoolSize = 50` creates $20 \times 50 = 1,000$ persistent TCP database connections.
      - A standard `db.r6g.large` PostgreSQL instance defaults to `max_connections = 100 - 200`.
      - PostgreSQL forks a distinct OS process per connection, each consuming 5–10MB of RAM for connection state, buffers, and session memory. 1,000 idle connections consume 5–10GB of database RAM purely on connection overhead, starving PostgreSQL of buffer cache and triggering `FATAL: remaining connection slots are reserved for non-replication superuser connections`.
    - **RDS Proxy Architecture**:
      - RDS Proxy maintains a small, fixed pool of backend connections to PostgreSQL (e.g., 50–100 connections).
      - It supports **Connection Pinning avoidance**: multiplexing client transactions over pooled connections when transactions are committed.
      - Applications can safely scale to hundreds of container replicas without overwhelming the database engine.
    - **HikariCP Tuning Invariant with RDS Proxy**:
      - Reduce container Hikari pool size (`maximumPoolSize = 5 - 10`).
      - Configure `maxLifetime` slightly shorter than the RDS Proxy idle client timeout (e.g., RDS Proxy default: 1800s $\rightarrow$ Hikari `maxLifetime = 1500000` ms).

??? example "Example"
    ```properties
    # HikariCP configuration optimized for Amazon RDS Proxy
    spring.datasource.hikari.maximum-pool-size=10
    spring.datasource.hikari.minimum-idle=5
    spring.datasource.hikari.max-lifetime=1500000
    spring.datasource.hikari.idle-timeout=600000
    spring.datasource.hikari.connection-timeout=30000
    spring.datasource.url=jdbc:postgresql://orders-proxy.proxy-xyz.us-east-1.rds.amazonaws.com:5432/orders
    ```

---

### 19. How do you synchronize an ALB Target Group's Deregistration Delay with Spring Boot's graceful shutdown lifecycle during zero-downtime deployments?

What causes intermittent HTTP 502 Bad Gateway errors during rolling updates if these parameters are misaligned?

??? question "Reveal answer"
    - **The Cause of HTTP 502 Errors**:
      - When an ECS rolling update starts, ECS calls `DeregisterTargets` on the ALB and simultaneously sends `SIGTERM` to the container task.
      - If Spring Boot shuts down immediately (or completes graceful shutdown in 10s while ALB Deregistration Delay is 30s):
        - The Spring Boot process exits and tears down its TCP socket.
        - Meanwhile, the ALB is still draining connections and may forward an in-flight HTTP request on an existing keep-alive TCP connection.
        - Because the container process is dead, the Linux kernel responds with a TCP `RST`, causing the ALB to return an immediate **HTTP 502 Bad Gateway** to the end client.
    - **Senior Synchronization Invariant**:
      $$\text{ALB Deregistration Delay} \ge \text{Spring Graceful Shutdown Timeout} + \text{Max Request Time}$$
      1. Configure Spring Boot:
         ```properties
         server.shutdown=graceful
         spring.lifecycle.timeout-per-shutdown-phase=25s
         ```
      2. Configure ALB Target Group `deregistration_delay.timeout_seconds = 30`.
      3. Configure container `stopTimeout = 35` seconds in the ECS task definition so Docker does not send `SIGKILL` prematurely.

??? example "Example"
    ```hcl
    # Terraform ALB Target Group deregistration delay tuning
    resource "aws_lb_target_group" "app_tg" {
      name        = "order-service-tg"
      port        = 8080
      protocol    = "HTTP"
      vpc_id      = aws_vpc.main.id
      target_type = "ip"

      deregistration_delay = 30

      health_check {
        path                = "/actuator/health/readiness"
        healthy_threshold   = 2
        unhealthy_threshold = 3
        interval            = 10
        timeout             = 5
      }
    }
    ```

---

### 20. How do you design a resilient cross-region disaster recovery architecture for an enterprise Spring Boot service on AWS?

Compare Active-Passive (Warm Standby / Pilot Light) versus Active-Active architectures in terms of RPO, RTO, and data consistency.

??? question "Reveal answer"
    - **Active-Passive (Pilot Light / Warm Standby)**:
      - *Architecture*: Primary region (e.g., `us-east-1`) serves 100% of production traffic. Secondary region (e.g., `us-west-2`) maintains asynchronous data replication (Aurora Global Database, S3 Cross-Region Replication, DynamoDB Global Tables). In Pilot Light, compute is stopped or kept at minimal scale (1 task). In Warm Standby, compute runs at reduced capacity (20%).
      - *Failover*: Route 53 DNS failover triggers during outage. Secondary Aurora cluster is promoted to standalone primary. ECS tasks scale up.
      - *RPO & RTO*: $RPO < 1\text{second}$ (asynchronous replication lag); $RTO = 5-15\text{minutes}$ (time to promote database and scale containers).
      - *Tradeoff*: Cost-effective; simple operational model without distributed write conflicts.
    - **Active-Active (Multi-Region)**:
      - *Architecture*: Both regions accept read and write traffic simultaneously. Route 53 latency-based routing directs users to the nearest region. Requires multi-region multi-master databases (e.g., Amazon DynamoDB Global Tables with conflict resolution via last-writer-wins).
      - *RPO & RTO*: $RPO \approx 0$; $RTO \approx 0$ (instant failover).
      - *Tradeoff*: Extremely complex and expensive. Relational databases (PostgreSQL/MySQL) cannot natively support low-latency active-active multi-region writes without cross-region roundtrip latencies ($60-80\text{ms}$) or asynchronous split-brain write conflicts.

??? example "Example"
    ```mermaid
    flowchart LR
        Users(["Global Users"]) --> R53["Route 53 (Health Check / Failover)"]
        
        subgraph RegionPrimary ["Primary Region: us-east-1 (Active)"]
            ALB1["ALB"] --> App1["ECS Tasks"]
            App1 --> AuroraPrimary[("Aurora Primary (Writer)")]
        end

        subgraph RegionSecondary ["Secondary Region: us-west-2 (Standby)"]
            ALB2["ALB"] --> App2["ECS Tasks (Minimal Scale)"]
            App2 --> AuroraSecondary[("Aurora Global DB (Read Only)")]
        end

        R53 -->|Primary Traffic 100%| ALB1
        R53 -.->|Failover on Health Check Alarm| ALB2
        AuroraPrimary -.->|Storage Replication < 1s lag| AuroraSecondary
    ```

---

### 21. How do AWS Organizations, Service Control Policies (SCPs), and Cross-Account IAM Roles enforce multi-account security boundaries in enterprise environments?

Why should production and non-production environments never share the same AWS account?

??? question "Reveal answer"
    - **Why Multi-Account Isolation Is Mandatory**:
      - **Blast Radius Containment**: A security breach or compromised credential in staging or development cannot access production databases or customer PII.
      - **Quota & Rate Limit Isolation**: High-throughput load tests or runaway scripts in staging cannot exhaust AWS API rate limits, VPC Elastic IPs, or EC2 service quotas needed by production.
      - **Cost Attribution**: Transparent financial billing and chargeback per environment or engineering domain.
    - **Service Control Policies (SCPs)**:
      - Applied at the AWS Organization root or Organizational Unit (OU) level.
      - Define maximum permissions that can be granted within member accounts. An SCP acts as a guardrail: even if an account administrator creates an `AdministratorAccess` policy with `"Action": "*"`, an SCP denying `s3:DeleteBucket` or denying actions in unapproved regions (e.g., outside `us-east-1` and `us-west-2`) **strictly overrides all account-level permissions**.
    - **Cross-Account IAM Role Assumption**:
      - Developers assume temporary IAM roles in specific accounts via AWS IAM Identity Center (SSO), producing audited STS audit trails in CloudTrail.

??? example "Example"
    ```json
    {
      "Version": "2012-10-17",
      "Statement": [
        {
          "Sid": "DenyUnapprovedRegions",
          "Effect": "Deny",
          "NotAction": [
            "iam:*",
            "route53:*",
            "cloudfront:*",
            "sts:*"
          ],
          "Resource": "*",
          "Condition": {
            "StringNotEquals": {
              "aws:RequestedRegion": [
                "us-east-1",
                "us-west-2"
              ]
            }
          }
        }
      ]
    }
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incident Scenarios (22–23)

### 22. Production Incident: A rolling deployment of a new Spring Boot service revision triggers a 3-minute burst of HTTP 502 Bad Gateway errors on the Application Load Balancer. The ECS tasks are green and passing health checks. What happened and how do you resolve it?

Walk through the diagnostic telemetry, root cause analysis, and architectural remediation.

??? question "Reveal answer"
    - **Incident Walkthrough**:
      - During a rolling deployment, ECS launched new tasks with container definition revision $v2$, waited for them to pass the ALB health check, and began terminating revision $v1$ tasks.
      - Immediately upon terminating $v1$ tasks, the ALB CloudWatch metric `HTTPCode_Target_5XX_Count` spiked with 502 Bad Gateway errors for 3 minutes.
      - ECS console showed all $v2$ tasks running healthy.
    - **Root Cause Analysis**:
      1. **Premature Socket Termination**: The Spring Boot application was configured with default Tomcat behavior (`server.shutdown=immediate`). When ECS sent `SIGTERM`, Tomcat closed its listening port immediately and dropped active TCP connections.
      2. **Mismatched ALB Deregistration Delay**: The ALB Target Group had `deregistration_delay.timeout_seconds = 300` (default). The ALB kept routing in-flight and keep-alive HTTP requests to the terminating container while Tomcat was already dying or dead.
      3. **Aggressive Health Checks vs Startup Time**: The target group had an overly aggressive health check interval (5s) without sufficient unhealthiness thresholds.
    - **Remediation Plan**:
      1. **Enable Graceful Shutdown in Spring Boot**:
         ```properties
         server.shutdown=graceful
         spring.lifecycle.timeout-per-shutdown-phase=25s
         ```
      2. **Tune ALB Target Group Deregistration Delay**:
         Reduce target group deregistration delay to `30s` in Terraform.
      3. **Tune Task Definition Stop Timeout**:
         Set `stopTimeout = 35` in the container definition so ECS does not send `SIGKILL` before Spring's 25-second graceful shutdown completes.
      4. **Add PreStop Hook (if on EKS)**:
         On EKS, add a `preStop: exec: command: ["/bin/sleep", "10"]` hook to allow Kubernetes endpoint controller propagation before `SIGTERM` is delivered.

??? example "Example"
    ```hcl
    # Production ECS container definition with tuned stopTimeout
    resource "aws_ecs_task_definition" "service" {
      family                   = "order-service"
      requires_compatibilities = ["FARGATE"]
      network_mode             = "awsvpc"
      cpu                      = "1024"
      memory                   = "2048"

      container_definitions = jsonencode([
        {
          name         = "order-app"
          image        = "123456789012.dkr.ecr.us-east-1.amazonaws.com/order-app:v2.0"
          stopTimeout  = 35
          essential    = true
          portMappings = [{ containerPort = 8080 }]
        }
      ])
    }
    ```

---

### 23. Production Incident: An Amazon RDS PostgreSQL Multi-AZ primary instance suffers a hardware hypervisor failure. The standby is promoted to primary within 75 seconds, but the Spring Boot backend remains down for 25 minutes throwing `SocketTimeoutException` and connection acquisition failures. Why did the service fail to recover, and how do you fix it?

Explain the failure mode involving JVM DNS resolution, HikariCP connection validation, and operating system TCP keepalives.

??? question "Reveal answer"
    - **Incident Walkthrough**:
      - At 02:14 UTC, AWS detected a physical hardware failure on the primary RDS instance in `us-east-1a`.
      - At 02:15:15 UTC (75s later), RDS successfully promoted the standby replica in `us-east-1b` and updated the canonical DNS CNAME record `orders.cluster-xyz.us-east-1.rds.amazonaws.com` to point to the new IP address in `us-east-1b`.
      - However, the 10 running Spring Boot backend instances continued throwing `HikariPool-1 - Connection is not available, request timed out after 30000ms` and `org.postgresql.util.PSQLException: The connection attempt failed` for 25 minutes until engineers restarted all ECS tasks.
    - **Root Cause Analysis**:
      1. **Infinite JVM DNS Caching (`networkaddress.cache.ttl = -1`)**: The Java runtime caches DNS lookups forever by default when a security manager is active, or relies on stale OS resolver caching. When HikariCP opened new socket connections to the database hostname, the JVM returned the *old IP address* of the dead primary instance in `us-east-1a`.
      2. **Zombie TCP Sockets & Missing Keepalives**: Existing pooled connections to the dead IP remained open at the TCP layer because the primary host crashed without sending TCP `FIN` or `RST`. Default Linux TCP keepalives take $> 2\text{hours}$ to detect a dead peer.
      3. **HikariCP `connectionTestQuery` Lag**: Because TCP sockets were hanging silently without timing out, Hikari connection threads blocked until `connectionTimeout` (30s) expired.
    - **Remediation Plan**:
      1. **Enforce JVM DNS Cache TTL**:
         Add JVM argument at startup:
         `-Dsun.net.inetaddr.ttl=5` or set `java.security.Security.setProperty("networkaddress.cache.ttl", "5");`.
      2. **Configure JDBC Driver Socket and Connection Timeouts**:
         Append explicit socket timeouts to the JDBC URL:
         `jdbc:postgresql://db.prod/orders?connectTimeout=10&socketTimeout=30&tcpKeepAlive=true`
      3. **Deploy Amazon RDS Proxy**:
         Placing RDS Proxy between Spring Boot and PostgreSQL handles failover at the proxy layer, reducing connection recovery time to $< 25\text{seconds}$.

??? example "Example"
    ```properties
    # Production Spring Boot DataSource configuration for fast RDS failover recovery
    spring.datasource.url=jdbc:postgresql://orders.cluster-xyz.us-east-1.rds.amazonaws.com:5432/orders?connectTimeout=5&socketTimeout=15&tcpKeepAlive=true
    spring.datasource.hikari.connection-timeout=10000
    spring.datasource.hikari.validation-timeout=3000
    spring.datasource.hikari.max-lifetime=900000
    # Enforce DNS cache TTL via JVM options in Dockerfile:
    # ENTRYPOINT ["java", "-Dsun.net.inetaddr.ttl=5", "-jar", "app.jar"]
    ```
<!-- --8<-- [end:scenarios] -->
