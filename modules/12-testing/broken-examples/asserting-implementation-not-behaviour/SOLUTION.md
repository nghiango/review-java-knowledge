# Solution: Checkout service test coupled to implementation

## Annotated code

### `CheckoutService.java`

```java
package lab.testing.broken.assertingimplementationnotbehaviour;

import java.util.List;
import lab.testing.pricing.LineItem;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import lab.testing.pricing.PricingCalculator;

/** Prices a basket and charges the customer through the payment gateway. */
public final class CheckoutService {

    private final PricingCalculator calculator;
    private final PaymentGateway gateway;

    public CheckoutService(PricingCalculator calculator, PaymentGateway gateway) {
        this.calculator = calculator;
        this.gateway = gateway;
    }

    public Money checkout(List<LineItem> items, int discountPercent) {
        Money subtotal = calculator.subtotal(items);
        Money total = calculator.applyPercentDiscount(subtotal, discountPercent);
        // Design issue: checkout() computes the discounted total but charges the pre-discount
        // subtotal. Nothing at the service boundary exposes the amount charged as an asserted
        // value, so the test below still passes while the customer is overcharged.
        gateway.charge(subtotal);
        return total;
    }
}
```

### `CheckoutServiceTest.java`

```java
package lab.testing.broken.assertingimplementationnotbehaviour;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import lab.testing.pricing.LineItem;
import lab.testing.pricing.Money;
import lab.testing.pricing.PaymentGateway;
import lab.testing.pricing.PricingCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    // Testing issue: PricingCalculator is a pure value collaborator that owns the pricing rule
    // under test. Doubling it removes the behaviour the test claims to verify, so the discount
    // rule is never executed and can be wrong without failing this test.
    @Mock private PricingCalculator calculator;

    @Mock private PaymentGateway gateway;

    @Test
    void checkoutUsesTheCalculatorThenChargesTheGateway() {
        List<LineItem> items =
                List.of(new LineItem("BOOK-001", 1999, 2), new LineItem("PEN-042", 250, 3));
        Money subtotal = new Money(4748);
        Money discounted = new Money(3798);
        when(calculator.subtotal(items)).thenReturn(subtotal);
        // Testing issue: the expected discount is supplied by the test itself, so the assertion can
        // only prove that the stub was called — never that the real percentage and rounding rules
        // produce this value.
        when(calculator.applyPercentDiscount(subtotal, 20)).thenReturn(discounted);

        CheckoutService service = new CheckoutService(calculator, gateway);
        service.checkout(items, 20);

        // Testing issue: assertions are coupled to the implementation, not to behaviour. Verifying
        // the exact argument lists, the internal subtotal call count and the call order pins the
        // test to today's delegation, not to the amount the customer is charged.
        verify(calculator).subtotal(items);
        verify(calculator).applyPercentDiscount(subtotal, 20);
        verify(calculator, times(1)).subtotal(anyList());
        // Design issue: the only externally visible effect of checkout — the amount charged — is
        // matched with any(Money.class), so the service boundary has no observable contract that
        // this test pins down.
        verify(gateway).charge(any(Money.class));

        // Maintainability issue: InOrder plus call-count verification breaks on any refactor that
        // preserves behaviour (reordering pure computations, caching the subtotal, extracting a
        // helper), producing false failures and pressure to weaken the test.
        InOrder inOrder = inOrder(calculator, gateway);
        inOrder.verify(calculator).subtotal(items);
        inOrder.verify(calculator).applyPercentDiscount(subtotal, 20);
        inOrder.verify(gateway).charge(any(Money.class));
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `CheckoutServiceTest.checkoutUsesTheCalculatorThenChargesTheGateway()` | Assertions verify internal call order, counts and exact arguments instead of the amount charged |
| 2 | Testing issue | High | `CheckoutServiceTest` (`@Mock PricingCalculator`) | Doubling the value collaborator replaces the pricing rule under test |
| 3 | Design issue | High | `CheckoutService.checkout()` / `CheckoutServiceTest` | The charged amount is matched with `any()`, so the boundary has no observable contract |
| 4 | Maintainability issue | Medium | `CheckoutServiceTest` (`InOrder`, `times(1)`) | Test fails on any refactor that preserves behaviour |

## Issue details

### Assertions coupled to implementation instead of behaviour

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5, Mockito · **Interview frequency:** High · **Production impact:** High

**Location:** `CheckoutServiceTest.checkoutUsesTheCalculatorThenChargesTheGateway()`

#### Problem
The test asserts *how* `CheckoutService` talks to its collaborators — the exact argument lists,
the number of `subtotal` calls and the call order — rather than *what* the checkout produces.

#### Why it happens
Mockito makes interaction verification easy, so a test written to "cover" a method often ends up
mirroring its body. The assertions restate the implementation instead of an externally observable
outcome, and a passing suite then feels like proof even though no business result is checked.

#### Production impact
```text
Implementation refactored, behaviour unchanged
→ verify(...) / InOrder expectations no longer match
→ test fails → developer weakens or deletes assertions
→ real regressions (e.g. the wrong amount charged) stay invisible
```

#### Broken implementation
```java
verify(calculator).subtotal(items);
verify(calculator).applyPercentDiscount(subtotal, 20);
verify(calculator, times(1)).subtotal(anyList());
InOrder inOrder = inOrder(calculator, gateway);
```

#### Correct implementation
```java
assertThat(charged).isEqualTo(new Money(3798));
verify(gateway).charge(new Money(3798));
```

#### Why the solution works
The assertions describe the contract a caller depends on: the amount returned and the amount
charged. Any implementation that produces those values passes; any that does not, fails.

#### Trade-offs
Behaviour assertions need a well-defined, observable result — which forced `checkout` to return the
amount charged. Where a boundary genuinely has no return value, assert on the recorded side effect
(for example the argument captured from the gateway) instead of on internal calls.

#### How to detect it
Look for `verify`, `times`, `InOrder` and argument-matcher imports in a test that never asserts a
return value or a captured argument. A quick check is to mutate a business rule and see whether the
test still passes.

#### Interview follow-up
> Give an example of a test that passes after a behaviour-preserving refactor but fails after a
> behaviour-changing one. What property must the test assert to achieve that?

#### Related
- Test doubles · Mockito `InOrder` · Behaviour-driven assertions · Test brittleness

### Mocking a value collaborator proves nothing

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Mockito, JUnit 5 · **Interview frequency:** High · **Production impact:** High

**Location:** `CheckoutServiceTest` (`@Mock PricingCalculator`)

#### Problem
`PricingCalculator` is a pure, in-process value collaborator, but the test replaces it with a mock
and stubs its result. The percentage and rounding rules that the test appears to exercise are never
executed.

#### Why it happens
Mocking is the default reflex for any injected dependency. A collaborator that performs no I/O is
mocked anyway, which silently deletes the code path under test and turns the test into a check that
a stub was called.

#### Production impact
```text
Discount rule changed / broken in PricingCalculator
→ CheckoutServiceTest still green (the mock returns whatever the test told it to)
→ wrong prices reach production with a green pipeline
```

#### Broken implementation
```java
@Mock private PricingCalculator calculator;
...
when(calculator.applyPercentDiscount(subtotal, 20)).thenReturn(discounted);
```

#### Correct implementation
```java
private final PricingCalculator calculator = new PricingCalculator();
...
assertThat(charged).isEqualTo(new Money(3798)); // real rule, real rounding
```

#### Why the solution works
The real calculator runs, so a broken percentage or rounding rule changes the asserted amount and
fails the test. Only the remote `PaymentGateway` — the boundary with I/O and non-determinism — is
doubled.

#### Trade-offs
Real collaborators make a test depend on more code, so a failure can originate in the collaborator.
Keep the collaborator pure and fast (as here) and reserve doubles for I/O boundaries, time and
randomness.

#### How to detect it
Every `@Mock` whose class does no I/O and holds no external state is a candidate. Ask: "if I break
this class, does the test still pass?" If yes, the mock removed the subject of the test.

#### Interview follow-up
> Which collaborators should be mocked and which should be real? How does the "mock only at the
> architectural boundary" rule change the shape of this test?

#### Related
- Test doubles · Sociable vs solitary tests · Value objects · Mockito strictness

### No observable behaviour at the service boundary

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5, Mockito · **Interview frequency:** High · **Production impact:** High

**Location:** `CheckoutService.checkout()` / `CheckoutServiceTest`

#### Problem
`checkout` has two observable outcomes — the amount it returns and the amount it charges — and the
test verifies neither precisely. The charge is matched with `any(Money.class)`, so the service can
charge the wrong amount (here the pre-discount subtotal) and the suite stays green.

#### Why it happens
The test was written around the implementation's call sequence rather than the service's contract.
When the only "result" a test knows about is a method invocation, the real outcome (money leaving
the customer's account) is never pinned down, so the boundary has no asserted behaviour at all.

#### Production impact
```text
Customer basket 4748 cents, 20% discount
→ CheckoutService charges 4748 (should be 3798)
→ 970 cents overcharged per order, test green, defect ships
```

#### Broken implementation
```java
gateway.charge(subtotal);                 // service: wrong amount
verify(gateway).charge(any(Money.class)); // test: accepts any amount
```

#### Correct implementation
```java
if (total.cents() > 0) {
    gateway.charge(total);
}
return total;
...
assertThat(charged).isEqualTo(new Money(3798));
verify(gateway).charge(new Money(3798));
```

#### Why the solution works
The service returns the amount it charged, and the test asserts both the return value and the
recorded charge. The contract is now expressed in money, which is the thing the caller and the
customer actually care about.

#### Trade-offs
Returning the charged amount couples the caller to it, which is usually desirable for a checkout but
must be kept consistent with the gateway's asynchronous settlement in a real payment provider.

#### How to detect it
Search tests for `any(`, `anyList()`, `anyString()` on values that represent the outcome. A matcher
that accepts "anything of the right type" is a place where the boundary's behaviour is unspecified.

#### Interview follow-up
> Design the `checkout` signature so its behaviour is fully observable without inspecting internal
> calls. What do you return when the basket is empty or fully discounted?

#### Related
- Test doubles · Contract testing · API design · Money value objects

### Test breaks on every behaviour-preserving refactor

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic
**Technology:** Mockito `InOrder` · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `CheckoutServiceTest` (`InOrder`, `times(1)`)

#### Problem
Verifying call order and the exact number of internal calls makes the test fail for changes that do
not alter what the customer is charged, for example caching the subtotal, extracting a helper or
reordering two pure computations.

#### Why it happens
`InOrder` and `times(n)` encode the current control flow. They were added to "be thorough", but they
assert an implementation detail that no caller can observe.

#### Production impact
```text
Refactor inside checkout (no behaviour change) → red test
→ developers spend time fixing the test, not the code
→ eventually the test is deleted, and the flow loses its only (weak) safety net
```

#### Broken implementation
```java
InOrder inOrder = inOrder(calculator, gateway);
inOrder.verify(calculator).subtotal(items);
inOrder.verify(calculator).applyPercentDiscount(subtotal, 20);
inOrder.verify(gateway).charge(any(Money.class));
```

#### Correct implementation
```java
assertThat(charged).isEqualTo(new Money(3798));
verify(gateway).charge(new Money(3798));
```

#### Why the solution works
No assertion depends on the sequence of internal calls, so the test tracks the observable contract
and survives any refactor that keeps the charged amount correct.

#### Trade-offs
Call-order verification is legitimate when the order itself is the contract — for example
"authenticate before authorise" or "commit before acknowledging a message". Use it only where the
sequence is observable or safety-critical.

#### How to detect it
Any test that fails after an IDE "extract method" or "inline variable" refactor without a behaviour
change is over-coupled. Run the refactor and observe.

#### Interview follow-up
> When is verifying call order justified in a test, and how do you keep such a test from becoming a
> change-detector?

#### Related
- Test brittleness · Change detector tests · Refactoring · Mockito `InOrder`

## Correct implementation

The production-ready counterpart lives in `lab.testing.pricing`:

- [`Money.java`](../../src/main/java/lab/testing/pricing/Money.java) — immutable cents value object.
- [`LineItem.java`](../../src/main/java/lab/testing/pricing/LineItem.java) — basket line record.
- [`PricingCalculator.java`](../../src/main/java/lab/testing/pricing/PricingCalculator.java) — pure
  subtotal and half-up percentage discount rules.
- [`CheckoutService.java`](../../src/main/java/lab/testing/pricing/CheckoutService.java) — validates
  the basket, charges only a positive total and returns the amount charged.
- [`PaymentGateway.java`](../../src/main/java/lab/testing/pricing/PaymentGateway.java) — the only
  collaborator a test should double.
- [`CheckoutServiceBehaviourTest.java`](../../src/test/java/lab/testing/pricing/CheckoutServiceBehaviourTest.java)
  — behaviour tests that use the real calculator and assert the returned and charged amounts.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).
