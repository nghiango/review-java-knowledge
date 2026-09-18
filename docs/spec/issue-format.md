# Broken Examples and Issue Format

Normative rules for broken examples, `SOLUTION.md`, inline issue comments and the global issue
catalogue under `docs/issues/`.

## 1. Why two views of every broken example

The developer must be able to **review the code first** and only then reveal the answers.
Therefore every broken example has:

| File | Purpose | Contains answers? |
|---|---|---|
| `*.java` (or Dockerfile, YAML, ...) | Clean, realistic review target — looks like a real pull request | **No.** No hint comments at all |
| `REVIEW.md` | The task: what to review, which dimensions to consider | No |
| `SOLUTION.md` | The same code reproduced with categorized inline issue comments, followed by the full issue list | Yes |

Never put issue comments into the review-target sources. The review target may contain ordinary
comments a real developer would have written (including misleading ones, if realistic).

## 2. Broken code rules

- Location: `modules/NN-<slug>/broken-examples/<example-name>/` only. Never in `src/`.
- Excluded from `./gradlew build` and `check`. Compiled only by `./gradlew compileBrokenExamples`
  (a separate source set rooted at `broken-examples/`, `compileOnly` dependencies, no tests) so the
  examples stay real code and do not rot after refactors.
- Package: `lab.<topic>.broken.<example>`.
- Must be realistic. Code compiles and "works" on the happy path but has engineering problems such
  as: race conditions, wrong transaction boundaries, SQL injection, N+1, insecure authorization,
  missing idempotency, resource leaks, pool exhaustion, wrong offset handling, infinite retries,
  blocking inside reactive pipelines, poor API semantics, deadlocks, bad caching, consistency bugs,
  thread starvation, missing validation, swallowed exceptions.
- Never silly (`int x = "hello";`). If a reviewer would not plausibly meet it in a PR, drop it.
- Each example contains **3–8 issues**, ideally spanning more than one category, so the exercise
  is a real review and not a spot-the-one-bug.
- Every broken example has exactly one corresponding correct implementation: a package in
  `src/main` for code modules; a corrected artefact in `broken-examples/<example>/correct/` for
  doc modules (module-conventions.md §4).
- Doc-module artefacts (Dockerfile, YAML, Markdown design docs) follow the same two-view rule.
  Issue comments in `SOLUTION.md` use the artefact's own comment syntax (`# Security issue: ...`
  for Dockerfile/YAML, `<!-- Design issue: ... -->` for Markdown) with the identical category
  string.

## 3. Inline issue comment format

Mandatory format, placed directly above the problematic statement in `SOLUTION.md` code blocks
(and in `docs/topics/<slug>/code-review.md` reveal sections):

```java
// Security issue: User-controlled input is concatenated directly into SQL,
// allowing SQL injection.
String sql = "SELECT * FROM users WHERE username = '" + username + "'";
```

```java
// Performance issue: Calling the repository inside the loop causes an N+1
// query problem and produces one query per order.
orders.forEach(order ->
    order.setCustomer(customerRepository.findById(order.getCustomerId()).orElseThrow()));
```

```java
// Concurrency issue: check-then-act is not atomic. Multiple threads can
// observe stock > 0 and decrement the same inventory concurrently.
if (stock > 0) {
    stock--;
}
```

```java
// Transaction issue: The remote HTTP call executes while the database
// transaction is open, keeping the DB connection occupied during network latency.
@Transactional
public void processOrder(...) {
    repository.save(...);
    paymentClient.charge(...);
}
```

Multiple problems on one block → multiple comments, one per issue.

### Allowed categories

Use exactly these strings (searchable across code, docs and catalogue):

```text
Security issue:
Performance issue:
Concurrency issue:
Transaction issue:
Data consistency issue:
Reliability issue:
Resilience issue:
Maintainability issue:
Design issue:
Architecture issue:
API design issue:
Database issue:
JPA issue:
Memory issue:
Resource leak issue:
Messaging issue:
Observability issue:
Scalability issue:
Testing issue:
Configuration issue:
Deployment issue:
```

## 4. `SOLUTION.md` structure

```markdown
# Solution: <example name>

## Annotated code

```java
// full review-target code with inline issue comments
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Transaction issue | High | `OrderService.processOrder()` | Remote call inside open transaction |
| 2 | Security issue | High | `OrderService.processOrder()` | Caller-supplied userId trusted |

## Issue details

One section per issue using the format in §5.

## Correct implementation

Code module: name the package (`lab.<topic>.<example>`) and link to
`src/main/java/lab/<topic>/<example>/`. Doc module: link to `correct/<artefact>`.
Both: link to `docs/topics/<slug>/solutions.md#<anchor>`.
```

## 5. Issue documentation format

Used in `SOLUTION.md`, `docs/topics/<slug>/code-review.md` and `docs/issues/*.md`.

```markdown
## External call inside transaction

**Type:** Transaction issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Spring Transactions, HikariCP · **Interview frequency:** High · **Production impact:** High

**Location:** `OrderService.processOrder()`

### Problem
The external payment request runs while the database transaction is open.

### Why it happens
Spring obtains a connection when the transactional method starts and keeps it bound to the
transaction until commit or rollback. The HTTP call may take seconds.

### Production impact
```text
100 concurrent requests → 100 open transactions → 100 checked-out connections
→ HikariCP exhausted → requests block → timeouts
```

### Broken implementation
(snippet)

### Correct implementation
(snippet)

### Why the solution works
Remote I/O happens outside the database transaction; transactions stay short.

### Trade-offs
Multiple transaction boundaries; partial failure must be handled explicitly
(pending state, retry, reconciliation job, Saga, transactional outbox).

### How to detect it
Hikari `pending` metric, `active == maximumPoolSize`, low DB CPU, thread dump shows threads
waiting in `HikariPool.getConnection`, slow-transaction logging.

### Interview follow-up
> What happens if the payment succeeds but the application crashes before the final transaction?

### Related
- Spring Transactions · HikariCP · Saga · Transactional Outbox · Idempotency
```

Metadata values (Severity, Difficulty, Interview frequency, Production impact) are learning
metadata, not measurements — use High / Medium / Low.

Issues that only occur on a specific track (e.g. `runBlocking` in a coroutine, Jackson 3
configuration mismatch) add **Track:** `kotlin` / `java25-boot4`. Baseline issues omit the field.
The catalogue's "Appears in" list may reference both baseline and track examples.

## 6. Global issue catalogue — `docs/issues/`

Problem-first navigation, independent of technology.

```text
docs/issues/
├── index.md            # catalogue overview, grouped by category
├── security.md
├── performance.md
├── concurrency.md
├── transaction.md
├── data-consistency.md
├── database.md
├── messaging.md
├── resilience.md
├── reliability.md
├── observability.md
└── maintainability.md
```

Every one of the 21 category strings in §3 maps to exactly one catalogue file. Categories without
a file of their own are filed under the closest problem family; the category string in the entry
heading stays exact so search still works:

| Category string | Catalogue file |
|---|---|
| `Security issue` | `security.md` |
| `Performance issue`, `Memory issue`, `Resource leak issue`, `Scalability issue` | `performance.md` |
| `Concurrency issue` | `concurrency.md` |
| `Transaction issue` | `transaction.md` |
| `Data consistency issue` | `data-consistency.md` |
| `Database issue`, `JPA issue` | `database.md` |
| `Messaging issue` | `messaging.md` |
| `Resilience issue` | `resilience.md` |
| `Reliability issue`, `Configuration issue`, `Deployment issue`, `Testing issue` | `reliability.md` |
| `Observability issue` | `observability.md` |
| `Maintainability issue`, `Design issue`, `Architecture issue`, `API design issue` | `maintainability.md` |

Do not create new catalogue files; extend this table (spec-first) if a category is added.

Each entry uses §5 and **must list every place it appears**:

```text
N+1 Query

Appears in:
→ JPA / Hibernate — Order loading             (modules/08-jpa-hibernate/broken-examples/n-plus-one)
→ REST API — Order details endpoint           (modules/10-rest-api/broken-examples/entity-leakage)
→ Performance — Database bottleneck scenario  (docs/scenarios/db-bottleneck.md)
```

This gives both directions: *Technology → Problems* and *Problem → Technologies where it occurs*.

## 7. Cross-cutting coverage

Across the whole repository the catalogue must end up containing realistic examples of at least:

- **Security:** SQL injection, authorization bypass / IDOR, secrets in logs, insecure configuration,
  missing validation, mass assignment, insecure CORS, unsafe deserialization concepts
- **Performance:** N+1, missing indexes, connection pool exhaustion, excessive allocation,
  lock contention, inefficient algorithms, oversized payloads
- **Concurrency:** race conditions, lost updates, deadlocks, visibility problems, incorrect locks,
  duplicate processing
- **Reliability / Resilience:** missing timeouts, unbounded retries, retry storms, duplicate
  messages, poison messages, partial failure
- **Data consistency:** lost updates, stale cache, dual writes (DB + broker), duplicate events,
  transaction boundary problems
- **Maintainability:** giant services, hidden dependencies, excessive coupling, duplicated logic,
  wrong abstraction
- **Observability:** missing logs, wrong log levels, missing metrics, missing trace context,
  high-cardinality metrics, swallowed exceptions
