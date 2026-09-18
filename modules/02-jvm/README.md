---
type: code
---

# Module 02 - JVM

Production-focused JVM examples covering bytecode, class loading, stack and heap pressure,
allocation, GC roots, container memory and class loader retention.

Study the canonical prose in [`docs/topics/jvm/`](../../docs/topics/jvm/) as it is built with this
module.

```bash
./gradlew :modules:02-jvm:test
./gradlew :modules:02-jvm:compileExamples
./gradlew :modules:02-jvm:compileBrokenExamples
```

This is a library/demo module and has no `bootRun` task. Diagnostic programs under
`src/examples/java` compile with the module but run only when invoked directly.
