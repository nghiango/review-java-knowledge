---
name: creating-module
description: Use when creating a new baseline module under modules/NN-<slug>/ or extending one that is missing required files, pages, or Gradle wiring
argument-hint: "<NN-slug>  e.g. 07-spring-transactions"
---

# Creating a Module

Scaffold and complete one baseline module so it can reach its Definition of Done.

**REQUIRED READING before writing anything:**
- `docs/spec/curriculum.md` — the module's entry (type, coverage, required broken examples)
- `docs/spec/module-conventions.md` §2–§5, §11–§12 (layout, module types, DoD)

## Steps

1. Look up the slug in the registry (`docs/spec/curriculum.md` → Slug registry). Confirm the
   type: **code** or **doc**. Never invent a slug; the same string is used for the directory,
   Gradle project, docs folder and package root.
2. Check what already exists: `glob modules/NN-<slug>/**` and `glob docs/topics/<slug>/**`.
   Only create what is missing.
3. Create the module skeleton:
   - code module: `README.md`, `build.gradle.kts` (convention plugins only, deps from the version
     catalog), `broken-examples/`, `src/main/java/lab/<pkg>/` (`<pkg>` = curriculum Package root
     column), `src/main/resources/`, `src/test/java`, `src/integrationTest/java`
   - doc module: `README.md`, `broken-examples/` (each example later gets a `correct/` folder —
     no `src/`, no Gradle project)
   - add `include("modules:NN-<slug>")` to `settings.gradle.kts` (code modules only)
   - note the required broken-example count from the curriculum entry; it overrides the DoD
     floor of 3 (code) / 2 (doc)
4. `README.md` is a pointer, not a textbook: purpose, how to run, link to
   `docs/topics/<slug>/index.md`. Declare `type: code|doc` in front matter.
5. Create the docs page set `docs/topics/<slug>/` (module-conventions §5). Doc modules omit
   `tests.md`.
6. Add the topic to `mkdocs.yml` nav, a row to `docs/progress.md`, a checklist to
   `docs/interview-checklist.md`.
7. Hand off: for each required broken example use `/writing-broken-example`; for questions use
   `/writing-interview-questions`; for docs content use `/documenting-topic`; finish with
   `/verifying-module`.

## Decision points

| Question | Answer |
|---|---|
| Is the baseline module for this slug already complete and I am asked for Java 25 / Kotlin? | Stop. Use `/creating-track-module`. |
| Module has no natural unit tests (JVM GC, leaks)? | Runnable `main` demo + JFR/heap-dump guidance in `production.md`; unit-test what is testable. |
| Does this module need Testcontainers? | Only if infrastructure semantics matter (DB, broker, cache). Put those tests in `src/integrationTest`. |
| Tempted to write theory in the module README? | Don't. Prose lives in `docs/topics/<slug>/`. |

## Common mistakes

- Creating `docs/topics/<slug>/` with a slug that differs from the module directory.
- Putting Lombok, H2 or field injection into `src/main`.
- Starting module N+1 before module N meets its DoD.
- Forgetting the `progress.md` row and `mkdocs.yml` nav entry — `mkdocs build --strict` will not
  catch an unlisted folder.
