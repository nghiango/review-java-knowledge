# Review: ThreadLocal pool leak

Review `RequestContext.java` and `RequestHandler.java` as a production pull request. The handler is
used by tasks submitted to a bounded executor, and downstream code reads the current user from
`RequestContext`.

Identify every issue you can find. Consider:

- request identity lifetime on reused pooled threads
- exception paths and cleanup
- nested request or impersonation scopes
- whether anonymous work can observe a previous authenticated user
- whether the API makes ownership visible to callers

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:02-jvm:compileBrokenExamples
```
