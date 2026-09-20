# Review: Checkout service test coupled to implementation

## Context

A team added `CheckoutService`, which prices a basket and charges the customer through
`PaymentGateway`. The companion unit test was written quickly, passes in CI, and has been treated
as the safety net for the checkout flow — yet a pricing defect has already shipped without the
suite noticing.

## Target Files

- [`CheckoutService.java`](CheckoutService.java)
- [`CheckoutServiceTest.java`](CheckoutServiceTest.java)

## Task

Review `CheckoutServiceTest.java` as if it were a pull request. Identify every problem with the test
and with the service boundary it exercises. Consider:

- what the assertions actually prove about the amount the customer is charged
- which collaborator is replaced by a test double, and whether that removes the rule under test
- how tightly the test is coupled to the service's internal call structure
- whether the test still passes when the implementation changes but the observable behaviour does not
- the behaviour the test never reaches (empty basket, full discount, invalid input)
- whether both observable outcomes of `checkout` — the returned amount and the charged amount — are
  verified

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
