# Project Specification

Normative specification for the Senior Java / Spring Interview Lab. The root `AGENTS.md` is the
short operating manual; these documents hold the detail.

| Document | Scope |
|---|---|
| [module-conventions.md](module-conventions.md) | Learning flow, module types (code / doc), directory layout, docs pages per topic, interview question format and minimum counts, code review flow, correct-implementation and test requirements, Definition of Done, progress and checklist formats |
| [issue-format.md](issue-format.md) | Two-view broken examples (clean review target + `SOLUTION.md`), broken-code rules, inline issue comment format and allowed categories, issue documentation format and metadata, global issue catalogue, cross-cutting coverage |
| [curriculum.md](curriculum.md) | Slug registry, module types, implementation order, per-module topic coverage and required broken examples for all 30 modules |
| [documentation-conventions.md](documentation-conventions.md) | MkDocs portal: site structure, home page, topic-first and problem-first navigation, code embedding via snippets, code-review page flow, before/after tabs, scenario pages, diagrams, writing style, `mkdocs.yml` baseline, docs Definition of Done, CI |
| [tracks.md](tracks.md) | Multi-version / multi-language tracks: registry (`java25-boot4`, `kotlin`), principles (baseline canonical, tracks are deltas, isolated included builds), layout, per-track coverage and broken examples, track Definition of Done, how to add or retire a track |

## Fixed decisions

| Decision | Choice |
|---|---|
| Baseline Java / Spring Boot | 21 / 3.5.x (pinned) in `modules/` |
| Newer versions & other languages | separate tracks under `tracks/<id>/` with their own pinned versions; tracks teach deltas only |
| Lombok | not used (Java) |
| Canonical prose location | `docs/` (modules hold code + short README) |
| Docker for `./gradlew build` | not required — unit tests only; `integrationTest` needs Docker |
| Broken examples in build | never; compiled on demand by `compileBrokenExamples` |
| Answers in review targets | never; only in `SOLUTION.md` and collapsed docs sections |
| Error contract | RFC 9457 Problem Details via `ProblemDetail` |

## Change process

Changing these specs is a deliberate act: update the spec first, then bring existing modules
into line, then update `docs/progress.md` if the Definition of Done changed.
