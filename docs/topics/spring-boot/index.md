# Module 05 — Spring Boot

Spring Boot is the industry-standard framework for building production-ready, cloud-native Java applications. It eliminates boilerplate configuration through opinionated starters, condition-driven auto-configuration, externalized hierarchical properties, production-grade telemetry (Actuator), and lifecycle-managed embedded runtimes.

---

## What This Module Covers

1. **Bootstrap Lifecycle**: `SpringApplication` startup sequence, event pipeline, environment preparation, and context initialization.
2. **Auto-Configuration Engine**: `@AutoConfiguration`, `AutoConfigurationImportSelector`, conditional evaluations (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`), and auto-configuration ordering.
3. **Starters Architecture**: Separation of starter aggregators and auto-configuration modules.
4. **Type-Safe Configuration**: `@ConfigurationProperties`, immutable records, Bean Validation (`@Validated`), relaxed binding, and the programmatic `Binder` API.
5. **Multi-Profile & Externalized Configuration**: 17-level property precedence hierarchy, multi-document YAML, profile activation expressions, and secure secret injection.
6. **Production Telemetry & Actuator**: Health indicators, Kubernetes readiness/liveness probes, metrics, environment masking, and endpoint security.
7. **Runtime & Container Lifecycle**: Embedded web servers, graceful shutdown coordination (`server.shutdown=graceful`), and Virtual Threads (`spring.threads.virtual.enabled=true`).
8. **Cloud-Native Testing**: `ApplicationContextRunner`, Testcontainers `@ServiceConnection`, and Docker Compose integration.

---

## Module Navigation

| Page | Purpose |
|---|---|
| [Concepts](concepts.md) | Architectural foundations, auto-configuration principles, and core features |
| [Internals](internals.md) | Deep dive into `SpringApplication`, `ConditionEvaluator`, `Binder`, and runtime phases |
| [Interview Questions](questions.md) | 23 categorized interview questions (Basic, Intermediate, Senior, Scenarios) with compilable code examples |
| [Code Review](code-review.md) | 5 realistic pull-request code review targets covering common security, concurrency, and configuration anti-patterns |
| [Solutions](solutions.md) | Production-grade reference fixes, design rationale, and trade-off evaluations |
| [Tests](tests.md) | Unit test suites, `ApplicationContextRunner` slice testing, and lifecycle verification |
| [Production Guide](production.md) | Production hardening, Actuator security audit, Kubernetes probe tuning, and shutdown diagnostics |
| [Exercises](exercises.md) | Hands-on engineering challenges with expandable solutions |

## Related

- [Spring Core](../spring-core/index.md)
- [Concurrency](../concurrency/index.md)
- [Security Issues](../../issues/security.md)
- [Reliability Issues](../../issues/reliability.md)
- [Maintainability Issues](../../issues/maintainability.md)
- [Interview Checklist](../../interview-checklist.md)
