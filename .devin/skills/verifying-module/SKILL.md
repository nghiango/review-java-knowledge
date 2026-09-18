---
name: verifying-module
description: Use before claiming a module, track module, or documentation change is complete, done, or ready to commit
argument-hint: "<NN-slug> [track-id]"
allowed-tools:
  - read
  - grep
  - glob
  - exec
permissions:
  allow:
    - Exec(./gradlew)
    - Exec(mkdocs)
    - Exec(scripts/verify-all.sh)
    - Exec(git status)
    - Exec(git diff)
    - Exec(ls)
---

# Verifying a Module

Evidence before assertions. Nothing is "done" until the commands below have been run **in this
session** and their output confirms it.

**REQUIRED READING:** `docs/spec/module-conventions.md` §11–§12 (Definition of Done);
`docs/spec/curriculum.md` entry for the module (required broken-example count); for tracks
`docs/spec/tracks.md` §4 (delta minimums) and §7.

## 1. Run the commands (all of them that apply)

```bash
./gradlew build                     # baseline, no Docker
./gradlew integrationTest           # Docker required — if Docker is unavailable, SAY SO explicitly
./gradlew compileBrokenExamples
./gradlew buildTrack-<id>           # when a track was touched (build + compileBrokenExamples of that track)
mkdocs build --strict
scripts/verify-all.sh               # instead of the above when the whole repo is claimed green
```

Paste the relevant tail of each output in the report. Do not summarise a command you did not run.

## 2. Static checks

```bash
# answers leaking into review targets (baseline AND tracks; SOLUTION.md is excluded by --include)
grep -rn " issue:" modules/*/broken-examples tracks/*/modules/*/broken-examples \
  --include=*.java --include=*.kt --include=Dockerfile --include=*.yml --include=*.yaml --include=*.json
# doc-module design docs / ADRs are Markdown review targets: check them separately
grep -rln " issue:" modules/*/broken-examples tracks/*/modules/*/broken-examples --include=*.md | grep -v SOLUTION.md
# hand-pasted Java in docs (should be snippets)
grep -rn "^public class\|^@Service\|^@Transactional" docs --include=*.md
# banned dependencies in Java code
grep -rn "lombok\|com.h2database" modules tracks/java25-boot4 --include=*.kts --include=*.toml
```

Every hit is a defect unless it is inside a spec file under `docs/spec/`.

## 3. Definition of Done walk-through

Tick each line of the DoD for the module type (code §11, doc §12, delta track module
`tracks.md` §4 table). For each unticked line either fix it or list it under "Not done" in the
report — never drop it silently.

Spot-check structurally:

- `ls broken-examples | wc -l` ≥ the number in the module's `curriculum.md` entry (e.g. 07 → 6),
  and the combined PR exercise exists for modules named in module-conventions §10
- every `broken-examples/<x>/` has `REVIEW.md`, `SOLUTION.md`, ≥ 1 clean source
- code module: every `broken-examples/<x>/` has a matching `src/main/java/lab/<pkg>/<x>/` package
  with tests (package name as recorded in that `SOLUTION.md`); doc module: has `correct/<artefact>`
- every issue in a `SOLUTION.md` appears in `docs/issues/*.md` (file per issue-format §6 table)
  with a back-link; track-specific issues carry `**Track:** <id>`
- question counts meet the minimums (8/8/5/2 code, 6/6/4/2 doc, 4/4/4/1 delta track module with
  ≥ 2 senior migration questions)
- track module: every docs page opens with the Delta admonition; `docs/tracks/index.md` matrix
  and the `## Track: <id>` table in `docs/progress.md` list the module
- `mkdocs.yml` nav, `docs/progress.md`, `docs/interview-checklist.md` updated

## 4. Report format

```text
Commands run: <list with pass/fail>
Static checks: <hits or "clean">
DoD: <n>/<total> — Not done: <list or "none">
progress.md updated: yes/no
```

## Red flags

- "Should pass" / "I'm confident" without output
- Marking `✅` in `progress.md` with unticked DoD lines
- Skipping `integrationTest` silently because Docker is missing
- Running only `./gradlew build` after changing docs
