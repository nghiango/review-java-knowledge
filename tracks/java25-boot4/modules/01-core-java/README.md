# Track Module: 01 — Core Java (Java 25 Delta)

This module covers the language and runtime **delta** between Java 21 and Java 25 LTS:

- **Stream Gatherers (`Stream::gather`)**: Custom intermediate operations (windowing, folding, scanning) transforming stream processing.
- **Flexible Constructor Bodies (JEP 482)**: Statements before `super(...)` and `this(...)` to validate inputs and prepare state before parent initialization.
- **Unnamed Variables & Patterns (JEP 456)**: Cleaner destructuring and exception handling using `_`.
- **Primitive Types in Patterns**: Pattern matching over primitive types (`int`, `long`, `double`).
- **Virtual Thread Synchronization Improvements**: Elimination of carrier thread pinning on `synchronized` blocks/methods.
- **Module Import Declarations (JEP 476)**: Simplified module imports (`import module java.base;`).

The canonical prose, migration guide, interview Q&A, and broken example analyses live in:

👉 **[Core Java Delta Documentation](../../../docs/tracks/java25-boot4/core-java/index.md)**
