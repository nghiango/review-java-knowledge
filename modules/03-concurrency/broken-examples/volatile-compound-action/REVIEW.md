# Review: Volatile used for compound check-then-act stock reservation

Review `ItemStock.java` and `InventoryReservationService.java` as a production pull request.
This service processes real-time inventory deductions during peak checkout flash sales.

Identify every issue you can find. Consider:

- the visibility vs atomicity semantics of `volatile` fields
- thread safety of check-then-act conditional operations
- concurrent registration and lookup in the item map
- handling of `InterruptedException`
- invariants under concurrent multi-item checkout requests

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:03-concurrency:compileBrokenExamples
```
