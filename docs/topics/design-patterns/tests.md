# Testing Design Patterns in Java & Spring

How to author robust, isolated unit tests for design patterns, verify execution order in decorator pipelines, and validate state machine transitions.

---

## 1. Testing Strategy & Factory Registries

When testing the **Strategy Pattern**, each strategy must be tested in total isolation without mocks, and the **Factory Registry** must be tested to ensure correct resolution and fast failure on unregistered types:

### Implementation: `StrategyPatternTest.java`

--8<-- "modules/29-design-patterns/src/test/java/lab/designpatterns/strategy/StrategyPatternTest.java"

### Verification Highlights
- Verifies that Credit Card and PayPal calculate their respective fee percentages and fixed charges accurately.
- Verifies that requesting an unregistered enum (`CRYPTO`) throws an explicit `UnsupportedOperationException`.

---

## 2. Testing Decorator Ordering & Security Invariants

Testing decorator compositions requires verifying both the positive path and security boundaries under cache hits:

### Implementation: `DecoratorOrderTest.java`

--8<-- "modules/29-design-patterns/src/test/java/lab/designpatterns/decorator/DecoratorOrderTest.java"

### Verification Highlights
- **Security Boundary Assertion**: Tests that once an Administrator populates the cache with a confidential order, a subsequent call from an unauthorized user (`GUEST`) is still rejected with a `SecurityException`, proving the authorization decorator runs *before* the cache lookup.
- **Cache Hit Assertion**: Tests that non-confidential orders return the exact same cached instance across multiple calls (`assertThat(secondCall).isSameAs(firstCall)`).

---

## 3. Testing State Machine Invariants

Testing the **State Pattern** requires asserting valid transitions and verifying that invalid lifecycle actions fail fast without corrupting internal state:

### Implementation: `StatePatternTest.java`

--8<-- "modules/29-design-patterns/src/test/java/lab/designpatterns/state/StatePatternTest.java"

### Key Testing Invariants
- `Draft` $\to$ `Paid` $\to$ `Shipped` succeeds sequentially.
- Attempting to ship an unpaid `Draft` order immediately throws `IllegalStateException`.
- Attempting to cancel an already `Shipped` order is rejected.

---

## 4. Testing Composable Specifications

Specifications should be tested across composite boolean branches (`and`, `or`, `not`):

### Implementation: `SpecificationPatternTest.java`

--8<-- "modules/29-design-patterns/src/test/java/lab/designpatterns/specification/SpecificationPatternTest.java"

---

## 5. Testing Chain of Responsibility

Testing a handler chain requires verifying both full traversal and early short-circuiting:

### Implementation: `ChainOfResponsibilityTest.java`

--8<-- "modules/29-design-patterns/src/test/java/lab/designpatterns/chain/ChainOfResponsibilityTest.java"
