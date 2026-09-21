# Solutions: Resilience Anti-Patterns in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline solutions in [`modules/18-resilience`](../../../topics/resilience/solutions.md) cover standard Resilience4j circuit breaker tune-ups and fallback definitions.
    This page details the refactoring steps and rationale for modern jittered retries, carrier-safe lock-free state machines, and concurrency limits.

---

## 1. Solution: Unbounded Retry Storm Without Jitter

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/18-resilience/broken-examples/unbounded-retry-storm-without-jitter/SOLUTION.md"
```

### Production Implementation: `ModernResilientExecutionEngine.java`
```java
--8<-- "tracks/java25-boot4/modules/18-resilience/src/main/java/lab/java25boot4/resilience/ModernResilientExecutionEngine.java"
```

---

## 2. Solution: Carrier-Pinning Circuit Breaker

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/18-resilience/broken-examples/carrier-pinning-circuit-breaker/SOLUTION.md"
```

### Production Implementation: `ModernResilientExecutionEngine.java`
```java
--8<-- "tracks/java25-boot4/modules/18-resilience/src/main/java/lab/java25boot4/resilience/ModernResilientExecutionEngine.java"
```
