# Core Java Tests

| Test | Behaviour proved |
|---|---|
| `CustomerKeyTest` | equality/hash lookup and validation |
| `CustomerSnapshotTest` | defensive copy and immutable output |
| `CustomerProfileServiceTest` | normalization, absence, invalid input and propagated outage |
| `CustomerCsvImporterTest` | closure on success/failure, classification, immutable result |
| `OrderReportServiceTest` | purity, order, context and executor concurrency bound |

```bash
./gradlew :modules:01-core-java:test
./gradlew :modules:01-core-java:test --tests '*streamprocessing*'
./gradlew :modules:01-core-java:compileBrokenExamples
```

No integration test is needed: these contracts require no database, broker or framework context.
Hand-written fakes expose behaviour directly, and latches coordinate concurrency without sleeps.

## Related

- [Solutions](solutions.md)
- [Test conventions](../../spec/module-conventions.md#9-tests)
