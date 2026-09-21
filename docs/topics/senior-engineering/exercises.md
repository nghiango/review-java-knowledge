# Senior Engineering Hands-On Exercises

Hands-on leadership exercises designed to practice authoring Architecture Decision Records (ADRs) and conducting constructive code reviews using Conventional Comments.

---

## Exercise 1: Author an Architecture Decision Record (ADR)

### Context & Requirements
Your e-commerce backend requires an asynchronous event bus to handle order lifecycle events (`OrderPlaced`, `PaymentProcessed`, `OrderShipped`). The events will be consumed by:
1. An inventory service (needs strict partition ordering per product ID).
2. A notification service (tolerates slight out-of-order delivery but requires high fanout).
3. A data warehouse ETL pipeline (requires historical event replay up to 30 days).

Peak throughput is estimated at $3,500\text{ events/sec}$. The team consists of 6 backend developers with existing AWS experience, but no dedicated Kafka cluster operators.

### Task
Write an Architecture Decision Record (ADR) adhering to the Michael Nygard format evaluating **Option A: Self-Hosted Apache Kafka on EC2/EKS**, **Option B: AWS Managed Streaming for Apache Kafka (Amazon MSK)**, and **Option C: AWS SQS + SNS Fanout**.

??? question "Reveal Solution: Model ADR"
    ```markdown
    # ADR 007: Adopt Amazon MSK for Asynchronous Order Event Streaming

    ## Status
    Accepted (Date: 2026-03-20)

    ## Deciders
    Staff Backend Engineer, Lead SRE, Backend Team Lead

    ## Context & Problem Statement
    Our order processing pipeline needs an asynchronous event bus to decouple the core checkout flow from inventory reservations, customer notifications, and long-term analytics ETL. The system must process up to 3,500 events/second, guarantee per-product partition ordering, and allow 30-day historical event replay.
    Our team has 6 backend developers and 1 SRE, with extensive AWS expertise but zero dedicated full-time Kafka administrators.

    ## Decision Drivers
    1. **Strict Partition Ordering**: Guarantee in-order processing per product ID.
    2. **Historical Event Replay**: Ability to re-read events up to 30 days for data science and service rebuilds.
    3. **Operational Simplicity**: Minimize multi-broker Zookeeper/KRaft operational overhead and 24/7 cluster patching.
    4. **Cost & Delivery Speed**: Fast setup using existing Terraform infrastructure modules.

    ## Considered Options
    1. **Option 1: Self-Hosted Apache Kafka on EKS**
    2. **Option 2: AWS Managed Streaming for Apache Kafka (Amazon MSK)**
    3. **Option 3: Amazon SNS + SQS Fanout**

    ## Pros and Cons of the Options

    ### Option 1: Self-Hosted Apache Kafka on EKS
    - *Good*: Lowest AWS infrastructure invoice cost.
    - *Bad*: Requires full-time SRE dedication for disk rebalancing, broker node patching, and backup management.
    - *Bad*: Significant operational risk for a 6-person team.

    ### Option 2: Amazon MSK (Chosen)
    - *Good*: Fully managed Kafka control plane with automated multi-AZ replication, EBS auto-scaling, and cluster patching.
    - *Good*: Supports consumer group partition ordering and configurable 30-day topic retention.
    - *Good*: Integrates seamlessly with Spring Kafka (`@KafkaListener`) and IAM authentication.
    - *Bad*: Higher monthly AWS managed service fee compared to raw EC2 instances.

    ### Option 3: Amazon SNS + SQS Fanout
    - *Good*: Serverless, near-zero operational maintenance.
    - *Bad*: Does not support event replay or log retention (SQS messages are deleted upon acknowledgement).
    - *Bad*: SQS FIFO throughput limits ($3,000\text{ msg/s}$ with batching) provide narrow headroom.

    ## Decision Outcome
    Chosen: **Option 2 (Amazon MSK)**.
    We accept the higher managed cloud cost in exchange for eliminating operational cluster management overhead, while achieving partition ordering and 30-day replay capabilities.

    ## Consequences
    - **Positive**: Core team focuses on domain event schemas (Avro/Schema Registry) rather than broker infrastructure.
    - **Negative**: Cloud infrastructure budget increases by approximately $1,200/month for the 3-node multi-AZ cluster.
    ```

---

## Exercise 2: Refactor Destructive Code Review Comments

### Context
A senior engineer submitted the following aggressive comments on a pull request that added a Spring `@Transactional` service with an unhandled third-party REST call inside the transaction:

```text
Reviewer Comment:
"Why are you calling the external PayPal API inside a @Transactional method?
This is completely brain-dead and will lock up all Hikari connections.
Didn't you read our engineering guidelines?
Rewrite this whole class immediately using an event listener or outbox pattern."
```

### Task
Transform this destructive, hostile comment into an exemplary review using the **Conventional Comments** specification. Ensure your rewritten comment explains the concrete failure mechanism, maintains psychological safety, and offers actionable code architecture guidance.

??? question "Reveal Solution: Model Review"
    ```markdown
    **blocking (reliability):** Calling the external PayPal REST API directly inside a `@Transactional` database method can lead to HikariCP connection pool starvation during payment gateway latency spikes.

    *Why:* `@Transactional` acquires a database connection from the pool at method entry and holds it open until the method exits. If the external PayPal API experiences a 5-second latency spike or network timeout, the database connection sits completely idle, blocking other customer requests from acquiring a connection. Under moderate traffic, this exhausts HikariCP and cascades into 504 Gateway Timeouts across the application.

    *Recommendation:* We should isolate the external HTTP call outside the database transaction boundary. A clean pattern here is:
    1. Persist the initial order state as `PENDING_PAYMENT` in a short, local database transaction.
    2. Make the outbound PayPal HTTP call outside the transaction.
    3. Transition the order to `PAID` in a second, fast database transaction upon receiving the gateway response.

    Alternatively, we can use the Transactional Outbox pattern or Spring's `@TransactionalEventListener(phase = AFTER_COMMIT)`.

    Happy to hop on a 15-minute pairing session if you'd like to walk through the boundary separation together!
    ```
