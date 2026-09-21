# AWS Production Operations & Incident Guide

Operational playbooks, real-world incident postmortems, telemetry monitoring standards, and the production readiness checklist for AWS backend systems.

---

## 1. Production Incident Postmortems

### Incident 1: The Single-AZ RDS Blackout

#### Incident Timeline
- **04:12 UTC**: AWS internal hypervisor failure causes an unexpected reboot of the underlying EC2 physical host hosting the primary RDS PostgreSQL database (`orders-db-production`) in `us-east-1a`.
- **04:14 UTC**: Because the database was provisioned as `multi_az = false`, there was no secondary standby replica in `us-east-1b`. AWS RDS control plane initiated automated host replacement and volume re-attachment.
- **04:22 UTC**: New host completes boot, but filesystem journal replay on the 500GB EBS volume detects corrupted block structures, requiring an automated `fsck` recovery process.
- **04:38 UTC**: PostgreSQL database completes crash recovery and begins accepting connections. Total customer outage: **26 minutes of complete transaction failure**.

#### Root Cause Analysis
1. The infrastructure had `multi_az = false` configured in Terraform to reduce monthly costs.
2. In a Single-AZ deployment, any hardware host failure requires AWS to find a new physical host in the same AZ, attach the existing EBS volume, boot the OS, and replay PostgreSQL WAL logs.
3. Under heavy write loads, uncheckpointed WAL records can cause crash recovery to take up to 20–40 minutes.

#### Remediation & Permanent Architectural Changes
1. Enabled `multi_az = true` in Terraform across all production databases ($RPO = 0$, automated standby promotion in $< 90\text{seconds}$).
2. Configured Amazon RDS Proxy to absorb connection drops and seamlessly redirect application queries to the promoted standby during failover.
3. Added CloudWatch Alarm on `DatabaseConnections` and `FreeStorageSpace`.

---

### Incident 2: The Secrets Invalidation Catastrophe

#### Incident Timeline
- **14:00 UTC**: An automated AWS Lambda rotation function executes as scheduled by AWS Secrets Manager to rotate the master PostgreSQL password.
- **14:00:15 UTC**: The Lambda successfully updates the PostgreSQL user password in the database and updates the secret JSON in AWS Secrets Manager.
- **14:02 UTC**: Because running Spring Boot applications cache the database password in their HikariCP DataSource in-memory, active connections continue working.
- **14:15 UTC**: A sudden traffic spike triggers an ECS auto-scaling policy, launching 15 new container tasks.
- **14:15:30 UTC**: New tasks fetch the new password from Secrets Manager and boot up successfully.
- **14:18 UTC**: The old 10 container tasks begin evicting connections reaching `maxLifetime` (30 minutes). When attempting to establish replacement connections, HikariCP reuses the *initial bootstrap password* passed via environment variables during container launch.
- **14:18:45 UTC**: PostgreSQL repeatedly rejects connection handshakes with `password authentication failed for user "app_user"`. The 10 old containers enter a connection retry loop, logging thousands of authentication errors and failing health checks.
- **14:22 UTC**: ALB marks the 10 old containers unhealthy and routes 100% of production traffic to the 15 new containers, which immediately suffer CPU saturation and connection exhaustion.

#### Root Cause Analysis
1. Environment variables injected into containers via ECS `secrets` are resolved **only once** when the container task starts. They are static throughout the container's lifetime.
2. When Secrets Manager rotates the password, existing containers cannot observe the new value without restarting or dynamically refreshing the `DataSource`.

#### Remediation & Permanent Architectural Changes
1. **IAM Database Authentication**: Replaced static database passwords with **AWS IAM Database Authentication**. Spring Boot generates short-lived (15-minute) signed AWS authentication tokens on-demand via the AWS SDK:
   ```java
   // Dynamic IAM Auth Token Generator for HikariCP Password Provider
   RdsUtilities rdsUtilities = RdsUtilities.builder().region(Region.US_EAST_1).build();
   String authToken = rdsUtilities.generateAuthenticationToken(builder -> builder
       .hostname("orders.cluster-xyz.us-east-1.rds.amazonaws.com")
       .port(5432)
       .username("iam_app_user")
       .build());
   ```
2. **Spring Cloud AWS Secrets Reload**: Configured Spring Cloud AWS Secrets Manager integration with event-driven reload triggers listening to EventBridge rotation events.

---

## 2. Critical CloudWatch Telemetry & Metrics

| Component | Metric | Alarm Threshold | Senior Diagnostic Meaning |
|---|---|---|---|
| **ALB** | `HTTPCode_Target_5XX_Count` | $> 1\%$ of total requests | Application unhandled exceptions, downstream timeouts, or container crashes. |
| **ALB** | `TargetResponseTime` | p99 $> 1500\text{ms}$ | Database lock contention, downstream dependency saturation, or thread pool starvation. |
| **ECS** | `CPUUtilization` | $> 80\%$ for 3 min | Approaching CPU starvation; trigger autoscaling. |
| **ECS** | `MemoryUtilization` | $> 85\%$ for 2 min | Memory leak or under-provisioned container limits; risks OOMKill. |
| **RDS** | `CPUUtilization` | $> 75\%$ for 5 min | Heavy sequential scans or unindexed queries; requires query analysis via Performance Insights. |
| **RDS** | `FreeStorageSpace` | $< 20\text{GB}$ | Risk of storage exhaustion which causes PostgreSQL to enter read-only mode. |
| **RDS** | `DatabaseConnections` | $> 80\%$ of `max_connections` | Connection leak or autoscaling explosion; deploy RDS Proxy immediately. |
| **SQS** | `ApproximateAgeOfOldestMessage` | $> 300\text{seconds}$ | Consumer processing bottleneck, deadlocked worker threads, or lag buildup. |
| **SQS** | `ApproximateNumberOfMessagesVisible` | Spiking $+100\%$ | Producer-consumer rate mismatch; trigger consumer autoscaling. |

---

## 3. Senior AWS Production Readiness Checklist

### Identity & Access Management (IAM)
- [ ] No IAM users with static long-lived access keys (`AKIA...`) exist for applications.
- [ ] ECS tasks use separate **Task Role** (runtime app permissions) and **Task Execution Role** (ECR/logging/secrets bootstrap).
- [ ] EKS pods use IAM Roles for Service Accounts (IRSA) with scoped OIDC subject conditions.
- [ ] Policies enforce least privilege: no wildcard `"Action": "*"` and no wildcard `"Resource": "*"`.
- [ ] Sensitive KMS operations enforce `kms:EncryptionContext` conditions.

### Compute & Resilience
- [ ] Compute workloads (ECS / EKS) are distributed across at least 2 distinct Availability Zones.
- [ ] Container definitions set `stopTimeout` ($\ge 30\text{s}$) to allow Spring Boot graceful shutdown to finish.
- [ ] Target group `deregistration_delay` is tuned to 20–30s to prevent HTTP 502s during deployments.
- [ ] Containers implement dedicated Actuator liveness (`/actuator/health/liveness`) and readiness (`/actuator/health/readiness`) endpoints.

### Database & Storage
- [ ] Relational databases (RDS / Aurora) are deployed with Multi-AZ automated failover enabled.
- [ ] Automated backup retention is set to $\ge 7\text{days}$ for continuous Point-In-Time-Recovery (PITR).
- [ ] Storage encryption at rest is enabled using an AWS KMS Customer Managed Key.
- [ ] Database instances reside strictly in Isolated Private Subnets with `publicly_accessible = false`.
- [ ] Deletion protection is enabled on production RDS instances.
- [ ] Amazon RDS Proxy is deployed to protect PostgreSQL from connection exhaustion during autoscaling bursts.

### Networking & Security
- [ ] VPC has private subnets routing through AZ-local NAT Gateways.
- [ ] Gateway Endpoints are configured for Amazon S3 and DynamoDB to eliminate NAT data processing fees.
- [ ] Application Load Balancer has AWS WAF enabled with Rate Limiting and OWASP Core Rule Sets.
- [ ] All HTTP traffic is redirected to HTTPS (TLS 1.3/1.2 enforced) with ACM certificates.
