# Review: Lock ordering deadlock and blocking network calls in account transfers

Review `Account.java`, `AuditNotificationClient.java`, and `AccountTransferService.java` as a production pull request.
This service orchestrates peer-to-peer balance transfers between customer accounts and issues audit notifications.

Identify every issue you can find. Consider:

- lock acquisition order between multiple shared resources
- behavior under bidirectional concurrent transfers (Account A → B and Account B → A)
- holding locks during third-party or network I/O calls
- self-transfer edge cases (transferring from an account to itself)
- timeout and deadlock recovery capabilities of intrinsic monitors

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:03-concurrency:compileBrokenExamples
```
