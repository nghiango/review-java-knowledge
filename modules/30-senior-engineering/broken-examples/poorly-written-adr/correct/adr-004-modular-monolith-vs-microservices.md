# ADR 004: Adopt Modular Monolith Architecture over Microservices Rewrite

## Status
Accepted (Date: 2026-03-15)

## Deciders
Architecture Working Group: Tech Lead, Staff Backend Engineer, Engineering Manager, Lead DevOps Engineer.

## Context & Problem Statement
Our e-commerce platform runs as a single Spring Boot application deployed to AWS ECS. Peak traffic reaches 350 requests per second with a p99 response time of 120ms. The engineering department consists of 8 backend engineers, 2 mobile engineers, 2 frontend engineers, and 1 DevOps engineer.

Over the past two quarters, engineering velocity has dropped:
- Gradle build and integration test execution takes 14 minutes in CI.
- Accidental cyclic dependencies between the `orders` and `billing` packages have caused unexpected regressions during deployments.
- Shared mutable database tables prevent independent domain schema changes.

A proposal was raised to perform a big-bang rewrite of the backend into 12 microservices. We must decide whether to transition to microservices or restructure the monolith while supporting our planned $2\times$ traffic growth over the next 18 months.

## Decision Drivers
1. **Developer Velocity**: Fast feedback loops, low cognitive overhead, and minimal local dev environment friction.
2. **Team Size & Operational Complexity**: Sustainable for a team of 8 backend engineers and 1 DevOps engineer.
3. **Domain Boundary Enforceability**: Clear compile-time or CI verification preventing circular dependencies.
4. **Zero-Downtime Scalability**: Ability to scale horizontally to 1,000 QPS without distributed transaction overhead.
5. **Cost Efficiency**: Minimize cloud compute, multi-cluster management, and network transfer costs.

## Considered Options
1. **Option 1: Big-Bang Microservices Rewrite (12 Services)**
2. **Option 2: Modular Monolith with Spring Modulith & ArchUnit Enforced Boundaries**
3. **Option 3: Strangler Fig Extraction of Payment Domain Only**

## Pros and Cons of the Options

### Option 1: Big-Bang Microservices Rewrite
- *Good*: Independent deployability of each microservice.
- *Good*: Distinct database schemas per service, enforcing bounded context isolation.
- *Bad*: Requires implementing distributed Sagas, Transactional Outbox, and dual writes across Kafka.
- *Bad*: Significant operational overhead: requires distributed tracing (OpenTelemetry), service mesh/Envoy, and Kubernetes management for 12 services.
- *Bad*: 8 backend engineers cannot sustainably maintain 12 separate CI/CD pipelines, on-call rotations, and repositories.
- *Bad*: High risk of project stagnation and feature freeze during a multi-quarter rewrite.

### Option 2: Modular Monolith with Spring Modulith & ArchUnit (Chosen)
- *Good*: Retains single deployment artifact, single database instance, and single CI/CD pipeline.
- *Good*: Strong modular encapsulation: Spring Modulith verifies package boundaries, event publication, and enforces zero circular references at test time.
- *Good*: In-process method calls and local Spring `@Transactional` semantics preserve ACID guarantees without distributed transaction latency.
- *Good*: Minimal cognitive and operational burden for a team of 8 engineers.
- *Good*: Cleanly prepares bounded contexts for future extraction if specific domains require independent autoscaling.
- *Bad*: All modules share JVM heap, CPU, and database connection pool. A memory leak or slow query in one module can degrade the entire application.
- *Bad*: Requires strict discipline to prevent database table joins across modules (enforced via ArchUnit rules).

### Option 3: Strangler Fig Extraction of Payment Domain Only
- *Good*: Isolates PCI-DSS compliance scope to a single dedicated service.
- *Bad*: Premature before fixing internal domain boundaries within the existing codebase.

## Decision Outcome
Chosen option: **Option 2: Modular Monolith with Spring Modulith & ArchUnit**.

We will restructure the existing Spring Boot application into distinct bounded contexts (`catalog`, `orders`, `inventory`, `billing`, `customer`) using Spring Modulith. Each module will expose only an explicit public API package; all internal entities and repositories will remain package-private.

### Implementation Guidelines
1. Introduce `spring-modulith-starter-core` and `spring-modulith-starter-test` to enforce module structure in `ApplicationModulesTest`.
2. Replace synchronous cross-module service calls with Spring application domain events where eventual consistency is acceptable.
3. Split the shared database into schema-isolated namespaces per module, strictly banning cross-schema SQL joins.

## Consequences

### Positive Consequences
- Immediate reduction in CI build times by isolating module test execution.
- Elimination of circular package dependencies and tangled domain logic.
- Avoided the \$150k+ estimated annual infrastructure and toolchain costs of a multi-cluster microservices deployment.
- Retained local development simplicity: single command `./gradlew bootRun` spins up the entire backend.

### Negative Consequences
- Team must learn and respect Spring Modulith event-driven conventions.
- High-scale background tasks (e.g. video processing or large PDF exports) still share JVM resources with customer-facing HTTP requests (mitigated via dedicated thread pools).
