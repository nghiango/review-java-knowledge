# Repository Architecture

## Build boundaries

The root Gradle build aggregates completed baseline modules. Convention plugins in `build-logic/`
provide one Java toolchain, test layout and quality policy without copying build scripts.

```text
root build
├── build-logic          shared policy
└── modules/01-core-java independent learning module
```

`test` is fast and Docker-free. `integrationTest` owns infrastructure-dependent semantics.
`brokenExamples` is an opt-in source set: it compiles through `compileBrokenExamples` but is never
part of `build` or `check`.

## Content boundaries

`modules/` is canonical code. `docs/` is canonical prose. MkDocs includes source files through
checked snippets, preventing documentation copies from drifting.

## Review-before-reveal

Every broken example has a clean source and `REVIEW.md` without hints. `SOLUTION.md` and a collapsed
portal section provide the categorized answer only after the learner has reviewed the source.

## Evolution

The baseline remains reproducible. Java 25 / Spring Boot 4 and Kotlin are isolated included-build
tracks that explain deltas rather than copying baseline lessons.

## Related

- [Module conventions](spec/module-conventions.md)
- [Tracks](spec/tracks.md)
