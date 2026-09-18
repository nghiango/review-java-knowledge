# Review: Excessive hot-path allocation in metric line encoder

Review `MetricLineEncoder.java` as a production pull request. This encoder is invoked on every
telemetry event (potentially millions of times per second across worker threads).

Identify every issue you can find. Consider:

- regular expression compilation frequency and caching
- short-lived object allocation inside the hot loop (streams, iterators, string formatting, arrays)
- logging volume and overhead on the critical path
- string replacement and escape efficiency
- data validation and encapsulation

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:02-jvm:compileBrokenExamples
```
