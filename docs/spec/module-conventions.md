# Module Conventions

Normative rules for every module in `modules/` and its documentation in `docs/topics/`.
See [AGENTS.md](../../AGENTS.md) for the short operating manual and
[curriculum.md](curriculum.md) for what each module must cover.

## 1. Project philosophy

For every topic the learning flow is:

```text
Theory
   ↓
Interview Questions
   ↓
Broken Example (clean review target)
   ↓
Developer reviews it
   ↓
SOLUTION.md with categorized inline issue comments
   ↓
Correct Implementation
   ↓
Tests
   ↓
Production Scenario
   ↓
Senior-level follow-up questions
```

Each topic must answer: What is it? Why does it exist? How does it work internally? When should
we use it / not use it? What commonly goes wrong? How is it detected? How is it fixed? What are the
trade-offs? How does it behave in production? What would an interviewer ask?

Do not teach APIs only. Always explain the underlying engineering principle.

## 2. Module types

| Type | Modules | Has `src/` + Gradle project | Has broken examples | Tests |
|---|---|---|---|---|
| **code** | 01–15, 18, 19, 21–23, 28, 29 | yes | yes, Java | unit + integration where infra semantics matter |
| **doc** | 16, 17, 20, 24, 25, 26, 27, 30 | no (16 may add an optional LocalStack sub-project) | yes, in the artefact's own language (Dockerfile, YAML pipeline, ADR, design doc) | none required; provide runnable demos or `docker compose` recipes where useful |

Module type is declared in `docs/spec/curriculum.md` and in the module's `README.md` front matter.

Both types also exist inside tracks (`tracks/<id>/modules/`), where they are additionally
**delta-scoped**: they cover only what differs from the baseline module with the same slug and
link to it for everything else. See [tracks.md](tracks.md).

## 3. Layout of a code module

```text
modules/07-spring-transactions/
├── README.md                         # short: purpose, how to run, link to docs/topics/spring-transactions/
├── build.gradle.kts                  # applies convention plugins only; declares deps from the catalog
├── broken-examples/
│   ├── external-call-in-transaction/
│   │   ├── REVIEW.md                 # the review task, dimensions to consider, NO answers
│   │   ├── OrderService.java         # clean review target(s), realistic PR-quality code
│   │   ├── PaymentClient.java
│   │   └── SOLUTION.md               # annotated code + complete issue list (see issue-format.md)
│   ├── self-invocation/
│   └── ...
└── src/
    ├── main/java/lab/springtransactions/
    │   ├── externalcall/             # correct implementation for the broken example above
    │   └── selfinvocation/
    ├── main/resources/               # application.yml, db/migration/ (Flyway)
    ├── test/java/...                 # unit tests (no Docker)
    └── integrationTest/java/...      # Testcontainers / @SpringBootTest
```

Rules:

- One correct-implementation package per broken example. The package name is the example
  directory name with dashes removed; it **may be shortened** to a recognisable prefix when the
  full name is unwieldy (`broken-examples/external-call-in-transaction` ↔
  `lab.springtransactions.externalcall`). The chosen name is recorded once, in the
  `## Correct implementation` section of that example's `SOLUTION.md`.
- Broken example sources use package `lab.<topic>.broken.<example>` (same `<example>` segment as
  the correct package) so that `compileBrokenExamples` can compile them without colliding with
  `src/main`.
- Module `README.md` is a pointer, not a textbook. All prose lives in `docs/topics/<slug>/`.
- Modules are independent Gradle projects; share only `modules/test-support` and `build-logic`.
- Each Spring module has a runnable `bootRun` that starts against `docker-compose.yml`
  infrastructure (`scripts/run-module.sh <slug>`).

## 4. Layout of a doc module

```text
modules/24-docker/
├── README.md
└── broken-examples/
    └── fat-image-root-user/
        ├── REVIEW.md                 # the review task, NO answers
        ├── Dockerfile                # clean review target
        ├── SOLUTION.md               # annotated artefact + issue list
        └── correct/
            └── Dockerfile            # corrected artefact (the doc-module equivalent of src/main)
```

Rules:

- Doc modules have no `src/` and no Gradle project. The corrected artefact lives in
  `broken-examples/<example>/correct/`, in the artefact's own language, and is referenced from
  `SOLUTION.md` and embedded in `docs/topics/<slug>/solutions.md` via snippets.
- Artefacts are not compiled, but they must be **valid** (a Dockerfile that builds, YAML that
  parses, an ADR that follows the template). `compileBrokenExamples` skips doc modules.
- Where a runnable check is cheap (`docker build`, `yamllint`, `hadolint`), note the command in
  `REVIEW.md` so the reviewer can run it.

Plus `docs/topics/docker/` with the same page set as a code module minus `tests.md`.

## 5. Docs pages per topic

```text
docs/topics/spring-transactions/
├── index.md              # overview, why it matters, navigation, progress badge
├── concepts.md           # core theory
├── internals.md          # how it works inside Spring/JVM/DB
├── questions.md          # basic → intermediate → senior → scenarios (see §6)
├── code-review.md        # one section per broken example: embedded clean code, review prompt, collapsed reveal
├── solutions.md          # correct implementation walkthrough, why it works, trade-offs
├── tests.md              # what the tests prove and how to run them (code modules only)
├── production.md         # production problems, diagnostics, checklist
└── exercises.md          # hands-on tasks + collapsed solutions
```

Code is embedded with `pymdownx.snippets`:

```markdown
```java
--8<-- "modules/07-spring-transactions/broken-examples/external-call-in-transaction/OrderService.java"
```
```

Never copy source into Markdown by hand.

## 6. Interview question format

Four levels per topic. Minimum counts for a complete module: **8 basic, 8 intermediate,
5 senior, 2 scenarios**.

| Level | Format |
|---|---|
| Basic | `Q`, **Short Answer** (≤ 5 lines), link to concept page |
| Intermediate | `Q`, **Short Answer**, **Internal Mechanism**, **Common Mistake**, links |
| Senior | full format below |
| Scenario | full format below, starts from symptoms/metrics, not from the concept |

Full format:

```text
Q: ...

Short Answer
Deep Explanation
Internal Mechanism
Example              (link/snippet to code in modules/)
Common Mistake
Production Consideration
Follow-up Questions
```

Every question links to its concept page and, where one exists, to the broken and correct
example. Questions are also surfaced globally under `docs/questions/` (see
documentation-conventions.md).

Example senior question (the anchor example for the whole repository):

> A method annotated with `@Transactional` saves an order, calls an external payment API taking
> 8 seconds, and then updates inventory. Under load the Hikari connection pool becomes exhausted.
> Explain why and redesign the flow.

## 7. Code review exercise flow

`REVIEW.md`:

```markdown
# Review: OrderService

Review `OrderService.java` and `PaymentClient.java` as if this were a pull request.

Identify every issue you can find. Consider:

- transactions
- concurrency
- security
- performance
- failure handling
- maintainability
- observability

Write your findings down before opening SOLUTION.md.
```

`SOLUTION.md` follows [issue-format.md](issue-format.md).

## 8. Correct implementation requirements

- Compiles, runs, has tests.
- Explains (in `docs/topics/<slug>/solutions.md` and in decision comments) why it fixes **each**
  issue listed in `SOLUTION.md`, and what trade-off it introduces.
- Production defaults: timeouts on all I/O, bounded retries with backoff + jitter, idempotency on
  anything retried or consumed from a broker, authorization checks, structured errors
  (`ProblemDetail`), meaningful metrics/logs.

Anchor example:

```java
public void processOrder(CreateOrderCommand command) {
    OrderId orderId = transactionService.createPendingOrder(command);
    PaymentResult result = paymentClient.charge(orderId, command.idempotencyKey());
    transactionService.completePayment(orderId, result);
}
```

with an explanation that remote I/O happens outside the DB transaction, and what the new
partial-failure modes are (pending state, retry, reconciliation, Saga, outbox).

## 9. Tests

- Tests demonstrate behaviour, edge cases, failure cases; concurrency, transaction, security,
  retry and messaging behaviour where applicable. Never write tests only for coverage.
- `src/test` — unit tests, no Docker, run by `./gradlew build`.
- `src/integrationTest` — Testcontainers (`@ServiceConnection`), `@SpringBootTest`, MockMvc,
  WireMock. Run by `./gradlew integrationTest`. Shared containers come from `modules/test-support`.
- Use Awaitility for asynchronous assertions. No `Thread.sleep`.
- For JVM/Core Java topics where a test cannot show the behaviour (GC, leaks), provide a runnable
  `main` demo plus JFR/heap-dump instructions in `production.md`, and unit-test whatever is testable.

## 10. Larger pull-request exercises

Some modules (at least 07, 08, 10, 11, 14, 19) include one **combined** broken example
(e.g. `broken-examples/order-processing-v1/`) mixing several categories: authorization, validation,
transaction boundary, N+1, publishing to Kafka inside the transaction, unbounded loading, error
contract. Its correct counterpart is a production-quality redesign using narrow transactions,
optimistic locking, idempotency, transactional outbox, bounded retries, structured errors,
observability. The combined exercise counts toward the module's curriculum minimum and is a
baseline requirement only — delta track modules do not need one.

## 11. Definition of Done — code module

```text
[ ] docs/topics/<slug>/ pages: index, concepts, internals, questions, code-review, solutions, tests, production, exercises
[ ] Questions: ≥ 8 basic, ≥ 8 intermediate, ≥ 5 senior, ≥ 2 scenarios
[ ] Broken examples: at least the number required by curriculum.md for this module, never fewer than 3 (each: clean sources + REVIEW.md + SOLUTION.md); modules listed in §10 include their combined PR exercise
[ ] Every SOLUTION.md issue registered in docs/issues/ with back-link
[ ] Correct implementation per broken example, with decision comments
[ ] Unit tests; integration tests where infrastructure semantics matter
[ ] Explanation of every fix + trade-offs in solutions.md
[ ] Production page: problems, diagnostics (metrics/logs/dumps), checklist
[ ] ≥ 2 exercises with solutions
[ ] mkdocs.yml nav, interview-checklist.md, progress.md updated
[ ] ./gradlew build, integrationTest, compileBrokenExamples, mkdocs build --strict all green
```

## 12. Definition of Done — doc module

```text
[ ] docs/topics/<slug>/ pages: index, concepts, internals, questions, code-review, solutions, production, exercises
[ ] Questions: ≥ 6 basic, ≥ 6 intermediate, ≥ 4 senior, ≥ 2 scenarios
[ ] Broken artefacts (Dockerfile, pipeline YAML, design doc, ADR, IAM policy, ...): at least the number required by curriculum.md, never fewer than 2; each with REVIEW.md + SOLUTION.md
[ ] Corrected artefact in broken-examples/<example>/correct/ with explanation and trade-offs in solutions.md
[ ] Production page with a real incident-style scenario
[ ] ≥ 2 exercises with solutions
[ ] mkdocs.yml nav, interview-checklist.md, progress.md updated
[ ] mkdocs build --strict green
```

## 13. Progress tracking

`docs/progress.md`:

```markdown
| # | Topic | Type | Theory | Q&A | Broken | Correct | Tests | Scenario | Docs |
|---|---|---|---|---|---|---|---|---|---|
| 07 | Spring Transactions | code | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
```

`⬜` not started · `🟨` in progress · `✅` complete. Doc modules use `—` in the Tests column.

One table per track below the baseline table (`## Track: java25-boot4`, `## Track: kotlin`),
listing only the modules that track implements.

## 14. Interview checklist

`docs/interview-checklist.md` has one checklist per topic, e.g.:

```markdown
## Spring Transactions

- [ ] Explain how @Transactional works internally
- [ ] Explain JDK proxy vs CGLIB
- [ ] Explain self invocation
- [ ] Explain propagation and isolation
- [ ] Explain rollback rules (checked vs unchecked)
- [ ] Identify bad transaction boundaries in code
- [ ] Explain Hikari exhaustion caused by long transactions
- [ ] Redesign external calls outside transactions
```
