# Code Review: Asynchronous Virtual Thread Test Flakiness & Shared State

## Context
A developer added a unit test to verify an asynchronous notification pipeline running on virtual threads.

## Files Under Review
- `OrderNotificationService.java`
- `AsyncOrderProcessingTest.java`

## Review Questions
1. How does using `Thread.sleep(50)` in `testOrderNotificationDispatched` behave when running on busy CI runners or when thread scheduling changes? What causes test flakiness?
2. What are the concurrency implications of using a `static final List<String> NOTIFICATIONS_LOG = new ArrayList<>()` across multiple test methods or concurrent test runners?
3. How should modern Java 25 tests synchronize with asynchronous tasks using Awaitility, CountDownLatch, or CompletableFuture without guessing arbitrary sleep durations?
