# Solutions: Testing Anti-Patterns in Spring Boot 4 & Java 25

!!! info "Delta from baseline"
    Baseline solutions in [`modules/12-testing`](../../../topics/testing/solutions.md) cover standard mock verification and unit test isolation fixes.
    This page details the refactoring steps and rationale for modern concurrency testing and REST assertion modernization.

---

## 1. Solution: Unbound Concurrency Test Flakiness

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/12-testing/broken-examples/unbound-concurrency-test-flakiness/SOLUTION.md"
```

### Production Implementation: `ModernOrderWorkflowEngine.java`
```java
--8<-- "tracks/java25-boot4/modules/12-testing/src/main/java/lab/java25boot4/testing/ModernOrderWorkflowEngine.java"
```

---

## 2. Solution: REST Test Client Assertion Drift

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/12-testing/broken-examples/rest-test-client-assertion-drift/SOLUTION.md"
```

### Production Implementation: `ModernOrderApiController.java`
```java
--8<-- "tracks/java25-boot4/modules/12-testing/src/main/java/lab/java25boot4/testing/ModernOrderApiController.java"
```
