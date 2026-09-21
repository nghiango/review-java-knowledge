# Testing Guide: Spring Boot 4 & Java 25 Test Suites

!!! info "Delta from baseline"
    Baseline testing guide in [`modules/12-testing`](../../../topics/testing/tests.md) covers standard unit tests and Spring slice configurations.
    This page covers testing patterns for virtual threads, Awaitility synchronization, and ArchUnit architectural compliance.

---

## 1. Concurrency Testing with Awaitility & Virtual Threads

Testing asynchronous logic on virtual threads requires poll-based synchronization to eliminate timing flakiness.

```java
@Test
@DisplayName("should execute subtasks concurrently and deterministically")
void shouldExecuteSubtasksConcurrently() throws Exception {
    ModernOrderWorkflowEngine engine = new ModernOrderWorkflowEngine(inventoryClient, fraudCheckClient, auditNotifier);

    var items = List.of(new ModernOrderWorkflowEngine.OrderItem("SKU-100", 2, 5000));
    var result = engine.executeOrderWorkflow("ord-123", items, Duration.ofSeconds(2));

    assertThat(result.orderId()).isEqualTo("ord-123");
    assertThat(result.inventoryReserved()).isTrue();

    // Verify asynchronous event recording deterministically
    await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
        assertThat(engine.getRecordedEventsCount()).isGreaterThanOrEqualTo(3);
    });
}
```

---

## 2. Endpoint Verification with RFC 9457 Assertions

Verify that endpoint exceptions produce compliant `ProblemDetail` structures:

```java
@Test
@DisplayName("should return RFC 9457 ProblemDetail 404 Not Found")
void shouldReturnProblemDetailForNotFound() throws Exception {
    mockMvc.perform(get("/api/v2/orders/ord-unknown")
            .accept(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Order Not Found"))
            .andExpect(jsonPath("$.detail").value(containsString("ord-unknown")));
}
```

---

## 3. ArchUnit Architectural Constraints

Verify that production packages follow module layering and do not depend on test fixtures or broken examples:

```java
@Test
@DisplayName("ArchUnit rule: production classes must not depend on broken examples")
void productionClassesMustNotDependOnBrokenExamples() {
    ArchRule rule = noClasses()
            .that().resideInAPackage("lab.java25boot4.testing")
            .should().dependOnClassesThat().resideInAPackage("..broken..")
            .allowEmptyShould(true);

    rule.check(importedClasses);
}
```
