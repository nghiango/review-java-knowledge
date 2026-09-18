# Review: Mutable customer risk-cache key

Review `CustomerKey.java` and `CustomerRiskCache.java` as a production pull request. The cache is
expected to find a customer's risk level throughout a profile refresh.

Identify every issue you can find. Consider:

- the `equals` / `hashCode` contract
- HashMap bucket selection and key stability
- encapsulation of mutable state
- inheritance and equality symmetry
- validation and failure behaviour

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:01-core-java:compileBrokenExamples
```
