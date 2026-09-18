---
name: reviewing-lab-change
description: Use when reviewing a diff, PR, or set of new files in this repository for compliance with the lab specification before merge or commit
allowed-tools:
  - read
  - grep
  - glob
  - exec
permissions:
  allow:
    - Exec(git diff)
    - Exec(git status)
    - Exec(git log)
---

# Reviewing a Lab Change

Review the change against the specification in `docs/spec/`, not against personal taste. Report
findings with file:line and the spec section violated.

## Procedure

1. `git diff --staged` (or `git diff`, or the files named by the user).
2. Classify what was touched: baseline module / track module / docs / spec / build.
3. Run the checklist below for each class. Read the referenced spec section when unsure.
4. Report: **Blocking** (spec violation) · **Should fix** · **Nit**, then the verdict.

## Checklist

**Broken examples** (`issue-format.md`)
- [ ] No `issue:` comments in review-target sources (any language: `.java`, `.kt`, Dockerfile, YAML, Markdown)
- [ ] `REVIEW.md` and `SOLUTION.md` present; SOLUTION issue table ↔ annotated comments match 1:1
- [ ] Categories are exact strings from the allowed list
- [ ] 3–8 realistic issues; would plausibly pass a rushed PR review
- [ ] Code module: matching correct package under `src/main` with tests, package name recorded in `SOLUTION.md`; doc module: `correct/<artefact>` inside the example
- [ ] Each issue registered in the `docs/issues/` file given by issue-format §6 table, with back-link; no new catalogue files
- [ ] Module's broken-example count still ≥ the curriculum requirement when examples are removed/renamed

**Correct code** (`AGENTS.md` §3, §7)
- [ ] No Lombok, H2, field injection; constructor injection, records, `final` fields
- [ ] Timeouts on external calls; bounded retries; idempotency where retried/consumed
- [ ] Authorization considered, not only authentication
- [ ] `ProblemDetail` error contract
- [ ] Decision/trade-off comments present; no syntax-explaining comments
- [ ] Unit tests in `src/test`, Testcontainers in `src/integrationTest`, no `Thread.sleep`

**Docs** (`documentation-conventions.md`)
- [ ] Code embedded via `--8<--`, not pasted
- [ ] Reveals collapsed; before/after in tabs
- [ ] `## Related` present; no re-explanation of a concept that has a canonical page
- [ ] Track pages start with a Delta admonition
- [ ] `mkdocs.yml` nav updated

**Questions** (`module-conventions.md` §6)
- [ ] Level format respected (basic short, senior full)
- [ ] Links to concept + broken + correct anchors

**Tracks** (`tracks.md`)
- [ ] Baseline module for the slug is complete
- [ ] Delta only; versions pinned in the track's own catalog
- [ ] Package root `lab.<segment>.<topic>` with the segment from `tracks.md` §1
- [ ] Delta-module minimums (`tracks.md` §4 table) met; ≥ 2 senior migration questions
- [ ] `Track:` metadata on track-specific issues

**Process**
- [ ] `docs/progress.md` updated honestly (no ✅ with open DoD items)
- [ ] Commit message explains *why*

## Report template

```text
Blocking
- modules/07-.../broken-examples/x/OrderService.java:42 — hint comment in review target (issue-format §1)

Should fix
- ...

Nit
- ...

Verdict: request changes | approve
```
