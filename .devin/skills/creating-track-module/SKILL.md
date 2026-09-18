---
name: creating-track-module
description: Use when working under tracks/ (java25-boot4, kotlin) or docs/tracks/, when asked for a newer Java/Spring Boot version or Kotlin variant of a topic, or when adding a new track
argument-hint: "<track-id> <NN-slug>"
---

# Creating a Track Module

A track module teaches the **delta** between the baseline stack and another one. It never
re-teaches what the baseline already covers.

**REQUIRED READING:** `docs/spec/tracks.md` (all); the baseline module's
`docs/topics/<slug>/` pages.

## Gate

```text
Does modules/NN-<slug> meet its Definition of Done?
  no  → stop; finish the baseline first (or pick a track-only module ≥ 40)
  yes → continue
```

## Steps

1. Confirm the track exists in the registry (`tracks.md` §1). If not, this is "adding a track":
   create `tracks/<id>/` as an included build (own `settings.gradle.kts`, `gradle/libs.versions.toml`,
   `build-logic/`, `test-support/`), register it in the table and in the root
   `settings.gradle.kts` (`includeBuild`), add `docs/tracks/<id>/index.md` and the overview pages
   listed in §3.
2. Diff the stacks for this topic. Write three lists before any code: **unchanged** (link to
   baseline), **changed**, **new**. This becomes the Delta admonition at the top of every docs page.
3. Create `tracks/<id>/modules/NN-<slug>/` with the same slug and number as baseline. Package
   root `lab.<segment>.<topic>` where `<segment>` is the **Package segment** column of
   `tracks.md` §1 (`java25boot4`, `kotlin`) and `<topic>` the curriculum Package root.
4. Broken examples: only ones that are **new or different on this stack** (e.g. Jackson 2 config
   under Jackson 3; `runBlocking` in a coroutine); ≥ 2 for a delta module. Same two-view
   contract → use `/writing-broken-example`. Tag issues with `**Track:** <id>` in `docs/issues/`.
5. Correct implementations use the new APIs/features; tests run on the track's toolchain.
6. Docs in `docs/tracks/<id>/<slug>/`, same page set as baseline, delta-scoped. Question
   minimums for a delta module: 4 / 4 / 4 / 1 with ≥ 2 senior migration/upgrade questions
   (`tracks.md` §4 table); track-only modules (≥ 40) use the full code-module DoD.
7. Update `docs/tracks/index.md` comparison matrix and the track section of `docs/progress.md`.
8. Verify with `./gradlew buildTrack-<id>` and `mkdocs build --strict`; baseline `./gradlew build`
   must be untouched and green.

## Kotlin-specific

Idiomatic Kotlin, not transliterated Java: `val`, null safety over `!!`, sealed + exhaustive
`when`, coroutines over blocking, `kotlin-spring`/`kotlin-jpa` plugins, MockK. The Java Lombok /
records rules do not apply. Data classes are **not** JPA entities.

## Common mistakes

- Copying the baseline module and editing it — the track becomes a fork that drifts.
- Putting Boot 4 or Kotlin versions in the root version catalog.
- A track docs page without a Delta admonition.
- Creating `tracks/<id>/modules/07-...` while baseline 07 is still 🟨.
