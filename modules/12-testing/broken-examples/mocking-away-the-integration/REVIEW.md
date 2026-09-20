# Review: Order fulfilment test that mocks away the integration

## Context

A team added `OrderFulfilmentService`, which asks the inventory service whether a requested quantity
is in stock, through `InventoryClient`. The companion unit test mocks the client, passes in CI and has
been treated as the safety net for the fulfilment flow — yet the inventory API's endpoint and response
field names have both changed since the test was written, and the suite still reports green.

## Target Files

- [`InventoryClient.java`](InventoryClient.java)
- [`OrderFulfilmentService.java`](OrderFulfilmentService.java)
- [`OrderFulfilmentServiceTest.java`](OrderFulfilmentServiceTest.java)

## Task

Review the three files as if they were a pull request. Identify every problem with the test and with
the client boundary it exercises. Consider:

- which collaborator is replaced by a test double, and what contract that removes from the suite
- whether the request path and the `Accept` header are ever checked
- whether the JSON field names the client and service rely on are ever verified
- what happens when the inventory service is slow, returns a 5xx or sends an unexpected body
- the type the client returns, and what each caller has to know in order to use it
- how a change to the inventory API's URL or payload would surface in the suite

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
