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

## Depth ladder

Every senior question should be able to climb:
What is X? → How does it work? → Why that way? → What goes wrong? → How to diagnose? → How to
fix? → Trade-offs? → At scale?

Anchor example: `@Transactional` → proxy → self invocation → boundary → connection lifecycle →
remote HTTP call → Hikari exhaustion → redesign → eventual consistency → Saga/Outbox.

## Steps

1. Read `docs/topics/<slug>/concepts.md` and the module's broken examples. Questions must
   reference things that exist in the repo.
2. For each question, create a **dedicated, compilable example class** under
   `modules/<NN-slug>/src/examples/java/lab/<topic>/questions/` (e.g. `Q01...Example.java`).
   The example code must demonstrate the runtime/execution outcome directly via trailing comments
   (e.g., `s1.equals(s2); // true`, `reader.readLine(); // "row"`).
3. Write basic → intermediate → senior → scenario, in that order, in `questions.md`. Wrap each
   answer in `??? question "Reveal answer"` and nest its code snippet in `??? example "Example"`.
4. For every question add links: concept page anchor; `code-review.md` anchor of the broken
   example; `solutions.md` anchor of the fix; diagram if any.
5. Surface into `docs/questions/<area>.md` via a snippet include (`--8<--`), never by copying.
6. Add the module's checklist items to `docs/interview-checklist.md`.

## Common mistakes

- Basic questions written in the 7-part format → walls of text. Keep them short.
- Senior questions that are just "explain X in depth". A senior question has a **situation**
  (load, failure, constraint) and asks for reasoning and redesign.
- Scenario questions that reveal the root cause in the question text.
- Answers that describe the API without the mechanism (e.g. "use `@Transactional`" without
  proxies/connection binding).
- Questions about features that have no example in `modules/` — add the example or drop the
  question.
