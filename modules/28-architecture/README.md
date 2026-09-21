# Module 28 — Architecture

This module contains practical implementations, unit tests, ArchUnit architectural fitness tests, and broken code review exercises for software architecture in Java and Spring:
- Layered, Hexagonal (Ports & Adapters), Onion, and Clean Architecture
- Modular Monolith (package boundaries, inter-module communication, Spring Modulith concepts) vs Microservices
- Domain-Driven Design (DDD): Strategic design (bounded contexts, context mapping) and Tactical design (aggregates, entities, value objects, domain events, domain services, application services)
- CQRS (Command Query Responsibility Segregation) and Event Sourcing foundations
- Architectural fitness verification using ArchUnit rules

Full theory, concepts, internal mechanics, interview questions, and deep walkthroughs live in the documentation:

👉 **[Architecture Documentation](../../docs/topics/architecture/index.md)**

## Structure

- `broken-examples/`: Intentionally flawed architectural implementations for code review practice:
  - `domain-depending-on-infrastructure/`: Domain entity directly coupled to JPA annotations, Spring dependencies, and external infrastructure
  - `anaemic-domain-with-god-service/`: Anaemic data model with procedural god service leaking business invariants and state transitions
  - `cross-module-database-access/`: Cross-bounded-context direct database queries breaking modular boundaries
- `src/main/java/lab/architecture/`: Production-grade correct implementations:
  - `cleanarchitecture/`: Pure domain core with decoupled incoming/outgoing ports and infrastructure adapters
  - `richdomain/`: DDD aggregate root protecting its invariants with immutable value objects
  - `modularmonolith/`: Segregated bounded contexts interacting strictly through public interfaces and domain events
- `src/test/java/lab/architecture/`:
  - `archunit/`: Automated ArchUnit fitness tests enforcing hexagonal boundaries, layered access, and modular slices
  - Domain unit tests verifying aggregate invariants without external mocks
- `src/examples/java/lab/architecture/questions/`: Dedicated compilable example classes for every interview question
