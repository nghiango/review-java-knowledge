---
name: writing-interview-questions
description: Use when writing or reviewing interview questions in docs/topics/<slug>/questions.md or docs/questions/, including migration questions for tracks
argument-hint: "<slug> [level]"
---

# Writing Interview Questions

Questions are the bridge between theory and code. Every question must be answerable at the depth
a Senior Java/Spring interviewer expects, and must link to where the concept and code live.

**REQUIRED READING:** `docs/spec/module-conventions.md` §6.

## Levels and minimums (code module)

| Level | Min | Format |
|---|---|---|
| Basic | 8 | Q · Short Answer (≤ 5 lines) · link to concept |
| Intermediate | 8 | Q · Short Answer · Internal Mechanism · Common Mistake · links |
| Senior | 5 | full 7-part format |
| Scenario | 2 | full format, **starts from symptoms/metrics**, not from the concept |

Doc modules: 6 / 6 / 4 / 2. Delta track modules (same slug as a baseline module): 4 / 4 / 4 / 1,
of which ≥ 2 senior are **migration/upgrade** questions. Track-only modules (≥ 40): code-module
counts + ≥ 2 migration questions.

Full format: Short Answer · Deep Explanation · Internal Mechanism · Example (link/snippet) ·
Common Mistake · Production Consideration · Follow-up Questions.

## Reveal contract

Only the question is visible on the page. Every answer sits inside exactly one collapsed
`??? question "Reveal answer"` admonition, with at least one nested, independently collapsed
`??? example "Example"` inside it:

```markdown
### Q: Why must equal objects have equal hash codes?

??? question "Reveal answer"

    **Short Answer:** Hash-based collections choose a bucket before checking equality. Equal
    objects with different hashes may never be compared.

    ??? example "Example"

        ```java
        --8<-- "modules/NN-<slug>/src/examples/java/<package>/<Example>.java"
        ```
```

- Basic keeps only Short Answer (≤ 5 lines) inside the reveal; Intermediate adds Internal
  Mechanism and Common Mistake; Senior/Scenario place all seven sections inside the same reveal.
- Nested examples come from compiled `src/examples/java` (package `lab.<topic>.examples`),
  tested `src/main/java` source, a clean broken review target (diagnose-it questions), or a
  shell command block for JVM diagnostics. Source always via `pymdownx.snippets` — never pasted.
- Verification counts `###` question headings, `??? question "Reveal answer"` blocks and nested
  `??? example "Example"` blocks per module — all three counts must match.

## Depth ladder

Every senior question should be able to climb:
What is X? → How does it work? → Why that way? → What goes wrong? → How to diagnose? → How to
fix? → Trade-offs? → At scale?

Anchor example: `@Transactional` → proxy → self invocation → boundary → connection lifecycle →
remote HTTP call → Hikari exhaustion → redesign → eventual consistency → Saga/Outbox.

## Steps

1. Read `docs/topics/<slug>/concepts.md` and the module's broken examples. Questions must
   reference things that exist in the repo.
2. Write basic → intermediate → senior → scenario, in that order, in `questions.md`, wrapping
   every answer in the reveal contract below.
3. For every question add links: concept page anchor; `code-review.md` anchor of the broken
   example; `solutions.md` anchor of the fix; diagram if any.
4. Surface into `docs/questions/<area>.md` via a snippet include (`--8<--`), never by copying.
5. Add the module's checklist items to `docs/interview-checklist.md`.

## Common mistakes

- Basic questions written in the 7-part format → walls of text. Keep them short.
- Senior questions that are just "explain X in depth". A senior question has a **situation**
  (load, failure, constraint) and asks for reasoning and redesign.
- Scenario questions that reveal the root cause in the question text.
- Answers that describe the API without the mechanism (e.g. "use `@Transactional`" without
  proxies/connection binding).
- Questions about features that have no example in `modules/` — add the example or drop the
  question.
