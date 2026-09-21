# Track: Java 25 & Spring Boot 4.0

This directory contains the **`java25-boot4` track**, an included Gradle build teaching the architectural and technical **delta** between the baseline stack (Java 21 + Spring Boot 3.5) and the newer Java 25 LTS / Spring Boot 4.0 stack.

## Architecture

- **Language & Runtime**: Java 25 LTS (`JavaLanguageVersion.of(25)`).
- **Framework**: Spring Boot 4.0.x / Spring Framework 7.x (Jakarta EE 11 baseline).
- **Package root**: `lab.java25boot4.<topic>` (e.g. `lab.java25boot4.corejava`).
- **Gradle**: Independent included build with its own `settings.gradle.kts`, `build-logic/`, and `gradle/libs.versions.toml`.

## How to Build

From the repository root:

```bash
./gradlew buildTrack-java25-boot4   # Builds this track
./gradlew buildTracks               # Builds all tracks
```

Or directly within this directory:

```bash
cd tracks/java25-boot4
../../gradlew build
../../gradlew compileBrokenExamples
```

## Documentation

Comprehensive guides, feature tours, migration runbooks, and senior interview Q&A live in:

👉 **[Java 25 & Spring Boot 4 Documentation](../../docs/tracks/java25-boot4/index.md)**
