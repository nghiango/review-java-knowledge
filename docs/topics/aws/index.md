# AWS Architecture for Backend Engineers

This module covers cloud architecture and AWS production engineering fundamentals for Senior Java / Spring Boot Backend Engineers. Rather than memorizing AWS certification trivia, this module focuses on real-world backend engineering: identity and access boundaries (IAM least-privilege, IAM Roles for Service Accounts / IRSA, ECS task roles), secure credential management (AWS Secrets Manager, SSM Parameter Store, KMS Customer Managed Keys), database resiliency (Amazon RDS / Aurora Multi-AZ failover, read replicas, connection pooling with RDS Proxy), compute selection tradeoffs (ECS Fargate vs EKS vs AWS Lambda with SnapStart), asynchronous event distribution (SQS, SNS, EventBridge), and resilient ingress (ALB vs NLB, cross-zone load balancing, health probes).

```mermaid
flowchart TD
    Client(["Public Clients"]) --> R53["Amazon Route 53 (DNS / Latency Routing)"]
    R53 --> ALB["Application Load Balancer (Multi-AZ)"]
    
    subgraph VPC ["Customer Amazon VPC (Multi-AZ: us-east-1a / us-east-1b)"]
        subgraph PublicSubnets ["Public Subnets"]
            ALB
            NAT["NAT Gateway (Outbound Egress)"]
        end
        
        subgraph PrivateSubnets ["Private App Subnets"]
            Fargate1["ECS Fargate / EKS Pod (AZ-a)<br/>Spring Boot 3.5"]
            Fargate2["ECS Fargate / EKS Pod (AZ-b)<br/>Spring Boot 3.5"]
        end
        
        subgraph IsolatedSubnets ["Isolated Database Subnets"]
            RDS_Primary[("RDS PostgreSQL Primary<br/>(Read / Write - AZ-a)")]
            RDS_Standby[("RDS PostgreSQL Standby<br/>(Sync Replica - AZ-b)")]
            RDS_Proxy["Amazon RDS Proxy<br/>(Hikari Connection Pooling)"]
        end
    end

    ALB -->|Target Group HTTP/8080| Fargate1
    ALB -->|Target Group HTTP/8080| Fargate2

    Fargate1 -->|JDBC Connections| RDS_Proxy
    Fargate2 -->|JDBC Connections| RDS_Proxy
    RDS_Proxy --> RDS_Primary
    RDS_Primary -.->|Synchronous Physical Replication| RDS_Standby

    Fargate1 --> SQS["Amazon SQS FIFO Queue<br/>(Order Events)"]
    Fargate2 --> SQS

    subgraph AWSServices ["AWS Managed Security & Telemetry"]
        SecMgr["AWS Secrets Manager<br/>(DB Password Rotation)"]
        KMS["AWS KMS (Customer Managed Key)<br/>Envelope Encryption"]
        CW["Amazon CloudWatch<br/>(Logs & Metric Alarms)"]
    end

    Fargate1 -.->|Task Role (IRSA)| SecMgr
    Fargate1 -.->|Decrypt Data| KMS
    Fargate1 -.->|awslogs Driver| CW
    SecMgr -.->|Uses CMK| KMS
    RDS_Primary -.->|Storage Encryption| KMS
```

---

## Core Production Invariants

| Invariant | Operational Rationale | Senior Production Standard |
|---|---|---|
| **IAM Least Privilege** | Prevent lateral movement, privilege escalation, and account takeover upon container compromise. | Never use wildcard actions (`s3:*`, `kms:*`) or wildcard resources (`Resource: "*"`). Scope policies strictly to specific ARNs, prefixes, and condition keys (`kms:EncryptionContext`). |
| **IAM Roles over Static Keys** | Eliminate hardcoded credentials and risk of long-lived access key leaks (`AKIA...`). | Use ECS Task Roles or EKS IAM Roles for Service Accounts (IRSA) with OIDC token federation. Never create IAM Users with permanent access keys for backend services. |
| **Secrets Externalization** | Prevent credentials from leaking via Git, task definition revisions, or process dumps. | Fetch secrets from AWS Secrets Manager or SSM Parameter Store with KMS CMK encryption. In ECS, reference secrets via `secrets` array with Task Execution Role decryption permissions. |
| **Multi-AZ by Default** | Survive single Availability Zone hardware, power, or networking failures without data loss. | Provision RDS PostgreSQL with `multi_az = true` ($RPO = 0$, automated failover in $< 120\text{s}$) and deploy compute workloads across at least 2 distinct Availability Zones behind an ALB. |
| **Connection Pooling with RDS Proxy** | Protect PostgreSQL from connection exhaustion caused by container autoscaling or Lambda bursts. | Place AWS RDS Proxy between Spring Boot / Lambda workloads and PostgreSQL to multiplex transactions and preserve database memory. |
| **Envelope Encryption with KMS** | Maintain full cryptographic isolation across microservice domains and meet regulatory compliance. | Encrypt all data at rest (EBS, S3, RDS, Secrets) using dedicated Customer Managed Keys (CMKs) with automated annual rotation and strict key policies. |

---

## Module Overview

| Resource | Purpose |
|---|---|
| [Concepts](concepts.md) | Compute models (ECS, EKS, Lambda), storage & DB, networking, IAM security, asynchronous messaging, and resilience |
| [Internals](internals.md) | RDS Multi-AZ replication mechanics, IAM STS token exchange (IRSA), ALB health check algorithms, and SQS visibility timeouts |
| [Interview Questions](questions.md) | 23 questions across Basic, Intermediate, Senior, and Production Incident Scenarios |
| [Code Review](code-review.md) | 3 broken review targets (IAM policy, ECS task definition, RDS Terraform) with collapsible issue reveals |
| [Solutions](solutions.md) | Production-grade IAM policies, secure ECS task definitions, and Multi-AZ Terraform configs with trade-offs |
| [Production](production.md) | Incident postmortems (The Single-AZ RDS Blackout, The Secrets Invalidation Catastrophe), telemetry alarms, and production checklist |
| [Exercises](exercises.md) | Hands-on exercises: Least-privilege IAM policy design & RDS failover resilience testing |

---

## Related

- [Docker](../docker/index.md) — Containerization, multi-stage builds, non-root security, and JVM container ergonomics
- [Performance](../performance/index.md) — HikariCP pool sizing, database query optimization, and latency percentiles
- [Distributed Systems](../distributed-systems/index.md) — Consensus, fault tolerance, and network partition recovery
- [Security issue catalogue](../../issues/security.md)
- [Reliability issue catalogue](../../issues/reliability.md)
- [Curriculum spec](../../spec/curriculum.md)
