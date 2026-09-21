# ADR 004: Migrate E-Commerce Backend to Microservices

## Status
Accepted

## Context
Our current Spring Boot monolith is too slow and getting too big. Microservices are the industry standard for modern cloud applications and will allow us to scale independently.

## Decision
We will rewrite the entire backend monolith into 12 microservices using Spring Boot, Spring Cloud, Kafka, and Kubernetes. Each team will own their own service and database.

## Consequences
Everything will be decoupled and we can deploy faster.
