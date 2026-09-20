# Review: Unbounded thread creation and unmanaged executors

Review `ReportGenerationWorker.java` and `BackgroundJobProcessor.java` as a production pull request.
This component processes report generation requests in background workers.

Identify every issue you can find. Consider:

- thread pool sizing and max worker constraints
- risk of `OutOfMemoryError: unable to create native thread` under traffic spikes
- unmanaged `new Thread()` creation vs managed executor pools
- missing thread naming, uncaught exception handling, and observability
- graceful shutdown vs abrupt worker termination

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:03-concurrency:compileBrokenExamples
```
