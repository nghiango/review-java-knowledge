# Review: Customer CSV import

Review `CustomerCsvImporter.java` as code that runs repeatedly in a long-lived batch service.
Callers expect either a clear import result or an I/O failure without corruption of their input.

Consider:

- ownership and lifetime of closeable resources
- collection iteration rules
- behaviour after a malformed row halfway through the file
- separation of parsing, validation and state mutation
- bounded memory and error reporting

Write your findings before opening `SOLUTION.md`.

```bash
./gradlew :modules:01-core-java:compileBrokenExamples
```
