# Review: Customer profile lookup

Review the profile service as production code used by an HTTP endpoint. It must distinguish invalid
input, a missing profile and an unavailable repository.

Consider:

- where Optional communicates absence clearly
- input and state contracts
- exception boundaries
- nullability and caller behaviour
- thread safety of singleton services

Write your findings before opening `SOLUTION.md`.

```bash
./gradlew :modules:01-core-java:compileBrokenExamples
```
