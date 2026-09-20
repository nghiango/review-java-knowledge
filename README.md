# Senior Java / Spring Interview Lab

A hands-on laboratory for preparing for **Senior Java / Spring Backend Engineer** interviews and
for sharpening day-to-day engineering judgement. Every topic is taught the same way:

```text
Theory → Interview questions → Broken code (review it yourself) → Reveal the issues
→ Correct implementation → Tests → Production incident scenario → Senior follow-ups
```

The repository is not a collection of snippets. Broken examples look like real pull requests,
correct implementations follow production practice, and every claim is backed by runnable code.

> **Status:** repository bootstrap and Modules 01 Core Java → 12 Testing are complete. Module 13
> Caching / Redis is next; later modules and tracks remain intentionally unscaffolded until their
> predecessors meet the Definition of Done. See [docs/progress.md](docs/progress.md) for verified status.

---

## What you get

| Asset | Where | Purpose |
|---|---|---|
| 30 topic modules (Core Java → System Design) | `modules/NN-<slug>/` | Correct, tested, production-style code |
| Broken examples (two views) | `modules/*/broken-examples/<example>/` | Review target with **no hints** + `SOLUTION.md` with categorised issue comments |
| Study portal (MkDocs Material) | `docs/` | Concepts, internals, questions, code-review pages, solutions, production pages, exercises |
| Global issue catalogue | `docs/issues/` | Problem-first navigation: "connection pool exhaustion" → every place it appears |
| Production scenarios | `docs/scenarios/` | Symptoms → metrics → logs → root cause → fix, answers collapsed |
| Interview questions | `docs/topics/<slug>/questions.md`, `docs/questions/` | Basic → intermediate → senior → scenario, linked to concept and code |
| Tracks | `tracks/java25-boot4/`, `tracks/kotlin/` | The **delta** for Java 25 + Boot 4 and for Kotlin — never a copy of the baseline |

### Stack

Baseline: **Java 21, Spring Boot 3.5.x**, Gradle Kotlin DSL with convention plugins, PostgreSQL,
Flyway, Spring Data JPA / JDBC, HikariCP, Spring MVC, Spring Security, Kafka, RabbitMQ, Redis,
Resilience4j, Micrometer + OpenTelemetry, Testcontainers, WireMock, ArchUnit, Docker Compose,
MkDocs Material. No Lombok, no H2 for behaviour, no field injection.

Curriculum (see [docs/spec/curriculum.md](docs/spec/curriculum.md) for coverage per module):

```text
01 Core Java            11 Spring Security          21 WebClient / WebFlux
02 JVM                  12 Testing                  22 Observability
03 Concurrency          13 Caching / Redis          23 Performance
04 Spring Core          14 Kafka                    24 Docker
05 Spring Boot          15 RabbitMQ                 25 AWS
06 Spring MVC           16 AWS Messaging            26 CI/CD
07 Spring Transactions  17 Distributed Systems      27 System Design
08 JPA / Hibernate      18 Resilience               28 Architecture
09 Database / SQL       19 Distributed Data Patterns 29 Design Patterns
10 REST API             20 Spring Cloud             30 Senior Engineering
```

Modules 16, 17, 20, 24–27, 30 are **doc modules**: their broken examples are Dockerfiles,
pipeline YAML, IAM policies, ADRs and design documents instead of Java.

---

## Using the lab as a learner

### 1. Prerequisites

- JDK 21 (tracks additionally need JDK 25 / Kotlin toolchains, provisioned by Gradle)
- Docker (only for `integrationTest` and `bootRun`; the unit-test build runs without it)
- Python 3 for the docs portal

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-docs.txt
mkdocs serve                      # http://127.0.0.1:8000
docker compose up -d              # PostgreSQL, Kafka, RabbitMQ, Redis for bootRun / integration tests
```

### 2. The study loop for one topic

1. Open the topic in the portal (`Topics → Spring Transactions`) and read `concepts` and
   `internals`.
2. Read the interview questions; try to answer before expanding the answers.
3. Open a broken example — either the `code-review` page or
   `modules/07-spring-transactions/broken-examples/<example>/` directly. Read `REVIEW.md`,
   then review the code **as a pull request**. Write your findings down.
4. Only then open `SOLUTION.md` (or the collapsed "Reveal issues" block). Each issue is tagged
   with a category such as `// Transaction issue:` and explains why it happens, its production
   impact, how to detect it and the trade-offs of the fix.
5. Study the correct implementation under `src/main/java/lab/<topic>/<example>/` and its tests:

   ```bash
   ./gradlew :modules:07-spring-transactions:test
   ./gradlew :modules:07-spring-transactions:integrationTest   # Docker
   scripts/run-module.sh spring-transactions                    # bootRun against docker-compose
   ```

6. Read the `production` page and the linked scenario; answer the senior follow-up questions.
7. Do the `exercises`; tick the topic in `docs/interview-checklist.md`.

### 3. Other ways in

- **Problem-first:** start at `Issues` in the portal — pick "N+1", "retry storm", "stale cache" —
  and follow the back-links to every module where it appears.
- **Incident-first:** start at `Scenarios` ("API p95 jumps to 9 s, Hikari pending = 75, DB CPU
  20 %") and reason your way to the root cause before expanding it.
- **Version delta:** finished a baseline topic? `Tracks → java25-boot4` or `Tracks → kotlin`
  shows only what changes on that stack, plus the senior migration questions.

### 4. Reading the code honestly

Review targets contain no answer comments by design. If you find a `// <Category> issue:`
comment outside a `SOLUTION.md`, that is a bug in the lab — please report it.

---

## Contributing (humans and AI agents)

The repository is specification-driven. Read, in this order:

1. [AGENTS.md](AGENTS.md) — the short operating manual: stack, layout, non-negotiable rules,
   verification commands, workflow checklist.
2. [docs/spec/](docs/spec/README.md) — the **normative** detail:

   | Spec | Read it when |
   |---|---|
   | [module-conventions.md](docs/spec/module-conventions.md) | Creating/extending a module, question format, Definition of Done |
   | [issue-format.md](docs/spec/issue-format.md) | Writing broken examples, `SOLUTION.md`, issue catalogue entries |
   | [curriculum.md](docs/spec/curriculum.md) | What each module must cover, required broken examples, slugs |
   | [documentation-conventions.md](docs/spec/documentation-conventions.md) | Anything under `docs/` or `mkdocs.yml` |
   | [tracks.md](docs/spec/tracks.md) | Anything under `tracks/` or `docs/tracks/` |

### Project skills

Workflows are encoded as skills in `.agents/skills/`. Invoke the matching one instead of
improvising; each names the spec sections it depends on.

| Skill | Use when |
|---|---|
| `/creating-module <NN-slug>` | Scaffolding a baseline module or filling missing files/pages/Gradle wiring |
| `/writing-broken-example <NN-slug> <example>` | Adding or changing anything under `broken-examples/` (both views, correct counterpart, catalogue registration) |
| `/writing-interview-questions <slug> [level]` | Writing or reviewing questions, including track migration questions |
| `/documenting-topic <slug> [page]` | Writing pages under `docs/topics/`, `docs/tracks/`, `docs/issues/`, `docs/scenarios/`, or touching `mkdocs.yml` |
| `/creating-track-module <track-id> <NN-slug>` | Working under `tracks/`, or asked for a Java 25 / Boot 4 / Kotlin variant of a topic |
| `/reviewing-lab-change` | Reviewing a diff or new files against the spec before commit |
| `/verifying-module <NN-slug> [track-id]` | Before claiming anything is done — runs the commands and walks the Definition of Done |

### Rules that are never bent

- Broken code lives only under `broken-examples/`, is never part of `./gradlew build`, and is
  compiled on demand by `compileBrokenExamples` so it stays real code.
- Every broken example has two views: a clean review target (no hints) and `SOLUTION.md`.
  Issue comments use exactly `// <Category> issue: <explanation>` with a category from
  `issue-format.md` §3; the same string is used in docs and the catalogue.
- Every broken example has a tested correct counterpart (`src/main` for code modules,
  `correct/` for doc modules) and an explanation of each fix **and its trade-off**.
- Docs embed code from `modules/` via `pymdownx.snippets` (`--8<--`); source is never pasted
  into Markdown.
- Modules are finished one at a time, in curriculum order, to their Definition of Done before the
  next one starts. Tracks only extend baseline modules that are already done.
- `docs/progress.md` is updated honestly — no `✅` with open Definition-of-Done items.

### Verification

```bash
./gradlew build                     # baseline compile + unit tests, no Docker
./gradlew integrationTest           # Testcontainers, Docker required
./gradlew compileBrokenExamples     # broken examples still compile (never part of build)
./gradlew buildTracks               # every track: build + compileBrokenExamples
./gradlew buildTrack-kotlin         # one track
mkdocs build --strict               # docs, no broken links or missing snippets
scripts/verify-all.sh               # all of the above
```

A change is complete only when all applicable commands are green, documented code still matches
the implementation, and `docs/progress.md` reflects reality.

---

## Layout

```text
.
├── AGENTS.md                       operating manual for contributors and agents
├── docs/                           canonical prose, MkDocs docs_dir
│   ├── spec/                       normative specification
│   ├── topics/<slug>/              one folder per baseline module
│   ├── tracks/<track-id>/          delta docs per track
│   ├── issues/                     global issue catalogue
│   ├── scenarios/                  production incident walkthroughs
│   └── questions/                  interview questions by area
├── modules/NN-<slug>/              baseline code: Java 21 + Boot 3.5.x
│   ├── broken-examples/<example>/  REVIEW.md · clean sources · SOLUTION.md [· correct/]
│   └── src/{main,test,integrationTest}/java/lab/<topic>/...
├── tracks/<track-id>/              isolated Gradle included builds (java25-boot4, kotlin)
├── build-logic/                    Gradle convention plugins
├── scripts/                        verify-all.sh, run-module.sh
└── .agents/skills/                 project workflows as skills
```

## Licence

To be decided before publication.
