---
type: code
---

# Module 29: Design Patterns

This module explores behavioral, creational, and structural design patterns in modern Java 21 and Spring Boot, focusing on practical backend applications, common anti-patterns (such as switch-on-type growth, pattern over-engineering, and decorator order bugs), and when *not* to use patterns.

## Canonical Documentation

For the full theory, internal mechanics, interview questions, and detailed code-review solutions, visit the documentation portal:

- [Design Patterns Overview & Concepts](../../docs/topics/design-patterns/index.md)
- [Core Theory & Pattern Catalog](../../docs/topics/design-patterns/concepts.md)
- [Spring Internals & Framework Implementation](../../docs/topics/design-patterns/internals.md)
- [Interview Questions](../../docs/topics/design-patterns/questions.md)
- [Code Review Targets](../../docs/topics/design-patterns/code-review.md)
- [Correct Solutions & Trade-offs](../../docs/topics/design-patterns/solutions.md)
- [Testing Patterns](../../docs/topics/design-patterns/tests.md)
- [Production Incidents & Checklist](../../docs/topics/design-patterns/production.md)
- [Hands-on Refactoring Exercises](../../docs/topics/design-patterns/exercises.md)

## Running Verification

```bash
# Compile and run unit tests
./gradlew :modules:29-design-patterns:test

# Compile broken examples (verifies broken code remains compilable)
./gradlew :modules:29-design-patterns:compileBrokenExamples

# Compile question demonstrations
./gradlew :modules:29-design-patterns:compileExamples
```
