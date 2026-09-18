# Review: Static listener leak

Review `GlobalEventRegistry.java` as a production pull request. The registry is intended to let
components subscribe to JVM-level diagnostic events and receive every published event.

Identify every issue you can find. Consider:

- listener lifecycle and ownership
- memory retention across redeployments or component shutdown
- duplicate registrations
- encapsulation of registry state
- behaviour under concurrent registration and publishing

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:02-jvm:compileBrokenExamples
```
