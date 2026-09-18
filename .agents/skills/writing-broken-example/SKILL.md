---
name: writing-broken-example
description: Use when adding or changing anything under broken-examples/ (review targets, REVIEW.md, SOLUTION.md), or when a correct implementation needs its broken counterpart
argument-hint: "<NN-slug> <example-name>"
---

# Writing a Broken Example

A broken example is a realistic pull request with 3–8 engineering problems, delivered in **two
views**: a clean review target and a `SOLUTION.md` that reveals everything.

**REQUIRED READING:** `docs/spec/issue-format.md` (all sections).

## The two-view contract

```text
broken-examples/<example-name>/
├── REVIEW.md        task + dimensions to consider        → NO answers
├── <artefact>       clean, realistic review target       → NO hint comments
├── SOLUTION.md      annotated code + issue table + details → answers live ONLY here
└── correct/         corrected artefact — DOC MODULES ONLY (code modules use src/main)
```

**Violating the letter of this contract is violating its spirit.** A single
`// Transaction issue:` comment in a review target turns the exercise into reading, not
reviewing.

## Step 0 — look up the module type

`docs/spec/curriculum.md` → Slug registry → **Type** column. It decides the shape of everything
below:

| | code module | doc module (16, 17, 20, 24–27, 30) |
|---|---|---|
| Review target | `*.java` (or `.kt` in the kotlin track), package `lab.<topic>.broken.<example>` | Dockerfile / YAML / Markdown design doc / ADR / IAM JSON — no package |
| Correct counterpart | `src/main/java/lab/<topic>/<example>/` + tests | `broken-examples/<example>/correct/<artefact>` — no tests |
| Issue comment syntax in SOLUTION.md | `// Security issue: ...` | artefact's own: `# Security issue: ...` (Dockerfile/YAML), `<!-- Design issue: ... -->` (Markdown) |
| Compile check | `./gradlew compileBrokenExamples` | artefact validity: `docker build`, `yamllint`, `hadolint` — note the command in REVIEW.md |

## Steps

1. Pick the example from the module's required list in `docs/spec/curriculum.md`. Directory
   name = kebab-case. Code modules: `<example>` package segment = directory name without dashes,
   optionally shortened (`external-call-in-transaction` ↔ `externalcall`); record the chosen
   name in the `## Correct implementation` section of `SOLUTION.md` and reuse it for
   `lab.<topic>.broken.<example>`.
2. Write the review target first, as a competent-but-rushed engineer would. Happy path works.
   Ordinary developer comments are allowed; hint comments are not.
3. Write `REVIEW.md` (module-conventions §7 template, file names adapted to the artefact): which
   files, which dimensions, any validity command, "write findings before opening SOLUTION.md".
4. Write `SOLUTION.md` (issue-format §4): the same code with `<Category> issue: ...` comments
   directly above each problem, then the issue table, then one §5 block per issue, then the link
   to the correct counterpart (Step 0 table) and the docs anchor.
5. Register every issue in `docs/issues/` — file chosen from the **category → file table in
   issue-format §6** (e.g. `Deployment issue` → `reliability.md`, `JPA issue` → `database.md`).
   Add the back-link ("Appears in"). Track-specific issues carry `**Track:** <id>`.
6. Create the correct counterpart (Step 0 table) and explain each fix and its trade-off in
   `docs/topics/<slug>/solutions.md`.
7. Run the compile/validity check from the Step 0 table — the review target must pass it.

## Category strings

Use exactly one of the strings listed in issue-format §3 (e.g. `Transaction issue:`,
`Security issue:`, `Messaging issue:`). Never invent variants (`Perf issue`, `TX issue`).
`docs/issues/` contains only the 11 files named in the §6 table — create a missing one from
that list if needed, never a file outside it.

## Red flags — stop and fix

- Any `issue:` comment in a review target (`*.java`, `*.kt`, Dockerfile, YAML, design doc)
  under `broken-examples/` — `SOLUTION.md` is the only file that may contain them
- Only one issue, or all issues in the same category
- A bug nobody would ship (`int x = "hello"`, obviously absurd names)
- `SOLUTION.md` lists an issue the annotated code does not mark, or vice versa
- Correct implementation exists but its `SOLUTION.md` counterpart is missing
- Broken example placed under `src/`; corrected artefact of a doc module placed anywhere but
  `broken-examples/<example>/correct/`
- A `docs/issues/<file>.md` whose name is not in the issue-format §6 table

## Rationalizations

| Excuse | Reality |
|---|---|
| "One small hint comment helps beginners" | It removes the exercise. Put hints in REVIEW.md dimensions instead. |
| "It doesn't need to compile, it's just for reading" | Rotting examples stop resembling real PRs. `compileBrokenExamples` exists for this. |
| "Registering in docs/issues can wait" | Problem-first navigation is a required learning mode; unregistered issues are invisible. |
