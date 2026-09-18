---
name: documenting-topic
description: Use when writing or editing pages under docs/topics/, docs/tracks/, docs/issues/, docs/scenarios/ or mkdocs.yml, or when code appears inside Markdown
argument-hint: "<slug> [page]"
---

# Documenting a Topic

`docs/` is the canonical home of all prose and is a MkDocs Material site. Code shown in docs is
always **included** from `modules/`, never pasted.

**REQUIRED READING:** `docs/spec/documentation-conventions.md`; for issue pages also
`docs/spec/issue-format.md` §5–§6.

## Page set per topic

`index` · `concepts` · `internals` · `questions` · `code-review` · `solutions` · `tests`
(code modules) · `production` · `exercises`. Every page ends with `## Related`.

## Non-negotiables

| Rule | How |
|---|---|
| Code from source, not copies | ```` ```java\n--8<-- "modules/NN-slug/…/File.java"\n``` ```` |
| Reveal is collapsed | `??? warning "Reveal issues"` — never annotated code above the fold |
| Question answers are collapsed | `??? question "Reveal answer"` holding the full answer, with a nested `??? example "Example"` snippet inside — heading/reveal/example counts must match per module |
| Before/after uses tabs | `=== "Broken"` / `=== "Correct"` with snippets |
| One canonical page per concept | Other pages link; they do not re-explain |
| Category strings identical everywhere | Same `Transaction issue` text in code, SOLUTION.md, docs, catalogue |
| Catalogue has 11 fixed files | Category → file per issue-format §6 table (`JPA issue` → `database.md`); never add a file |
| Track pages open with a Delta admonition | `!!! info "Delta from baseline"` — unchanged / changed / new |
| Consistent search terms | `@Transactional`, self invocation, Hikari, N+1, idempotency, … |

## questions.md reveal contract

Only the question shows on the page. The complete answer sits inside one collapsed
`??? question "Reveal answer"` admonition; its example is a second, independently collapsed
admonition nested inside it:

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

Example source comes from compiled `src/examples/java` (`lab.<topic>.examples`), `src/main/java`,
a clean broken review target, or a shell command block — always via `pymdownx.snippets`, never
pasted.

## code-review.md flow (per broken example)

1. Context: what the code should do (2–4 lines)
2. Embedded clean review target
3. Review prompt (dimensions from `REVIEW.md`)
4. `??? warning "Reveal issues"` containing the §5 issue blocks
5. Link to the `solutions.md` anchor

## Scenario pages (`docs/scenarios/`)

Symptoms → Metrics → Logs → Hypotheses → Root cause → Broken code → Investigation → Fix →
Verification → Prevention → Interview questions. Answers collapsed in `??? note` blocks.

## Style

Concept → Example → Failure → Fix → Trade-off → Interview question. Lists, tables and Mermaid
over paragraphs. Show the broken flow **and** the corrected flow when diagramming an issue.

## Finish

- Add the page to `mkdocs.yml` nav
- `mkdocs build --strict` must be green (missing snippet paths fail thanks to `check_paths: true`)

## Common mistakes

- Relative links to `../../modules/...` — they break under `--strict`. Use snippets or the
  configured `repo_url`.
- Pasting a method into Markdown "just this once" — it will drift.
- Explaining `@Transactional` again on the Hikari page instead of linking.
- A reveal section that repeats the issue table but omits *Why it happens* / *How to detect it*.
