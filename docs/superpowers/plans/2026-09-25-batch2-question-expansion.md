# Batch 2 Curriculum Question Expansion (Modules 06–10) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand baseline modules 06 to 10 (Spring MVC, Spring Transactions, JPA/Hibernate, Database/SQL, REST API) from 23 to 30 canonical interview questions each (total 35 new questions) with dedicated compilable Java example classes, integrating questions from `temporary-practise.md` into JPA/Hibernate, and establishing enriched cross-module follow-up links.

**Architecture:** Each module receives +2 Intermediate, +3 Senior, and +2 Scenario questions according to the curriculum convention. Every question is backed by a compilable Java class under `modules/<NN-slug>/src/examples/java/lab/<topic>/questions/` (named `Q24...` through `Q30...` to preserve existing references) with trailing evaluation comments and is referenced via PyMdown snippets in `docs/topics/<slug>/questions.md`. Senior and Scenario questions are enriched with bidirectional cross-links to sibling modules in their `**Follow-up Questions:**` sections.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Gradle Kotlin DSL, Spotless (Google Java Format), MkDocs Material.

**Spec:** `docs/superpowers/specs/2026-09-25-curriculum-question-expansion-design.md`

## Global Constraints
- Every question must compile with zero errors via `./gradlew compileExamples`.
- All snippet references (`--8<--`) must resolve and `mkdocs build --strict` must pass with zero broken links.
- Follow the 7-part format for Senior and Scenario questions verbatim: Short Answer, Deep Explanation, Internal Mechanism, Example, Common Mistake, Production Consideration, Follow-up Questions.
- Follow-up questions must include relative markdown links (`/topics/...`) to target module anchors.
- Trailing comments in example classes must explicitly state evaluation outcomes.

## Review Focus
1. Link Anchor Validity: Relative paths and anchor names in `**Follow-up Questions:**` must point to existing heading slugs in target topic `questions.md` files so `mkdocs build --strict` will not fail.
2. Snippet Inclusion Accuracy: PyMdown snippet paths `--8<-- "modules/..."` must match the exact file name and path of newly created example classes.
3. Example Compilation Under Java 21: New example classes must compile under standard javac without Error Prone warnings.
4. Formatting Integrity: Adhere to Google Java Format via `./gradlew spotlessApply`.
5. JPA & Transactions Synergy: Explicitly integrate OSIV lazy loading and pagination join fetch from `temporary-practise.md` into Module 08 (JPA/Hibernate) and cross-link with Module 07 (Spring Transactions).

---

### Task 1: Module 06 — Spring MVC Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q24StreamingResponseBodyExample.java`
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q25MultipartUploadSecurityExample.java`
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q26ForwardedHeaderSecurityExample.java`
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q27ProtobufHttpMessageConverterExample.java`
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q28AsyncTimeoutHandlingExample.java`
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q29TomcatWorkerExhaustionScenarioExample.java`
  - `modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q30JsonCircularReferenceScenarioExample.java`
- Modify:
  - `docs/topics/spring-mvc/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.springmvc.questions.*` and 7 new questions in `docs/topics/spring-mvc/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 06**
  Create `Q24StreamingResponseBodyExample.java` (asynchronous streaming via StreamingResponseBody), `Q25MultipartUploadSecurityExample.java` (file size bounding, MIME validation, path traversal prevention), `Q26ForwardedHeaderSecurityExample.java` (ForwardedHeaderFilter prevents host header poisoning in UriComponentsBuilder), `Q27ProtobufHttpMessageConverterExample.java` (custom HttpMessageConverter negotiation), `Q28AsyncTimeoutHandlingExample.java` (DeferredResult timeout and error callbacks), `Q29TomcatWorkerExhaustionScenarioExample.java` (Tomcat maxThreads exhaustion under downstream blocking I/O), and `Q30JsonCircularReferenceScenarioExample.java` (bidirectional entity relationships causing Jackson infinite recursion).

- [ ] **Step 2: Append Intermediate Q24 and Q25 to `docs/topics/spring-mvc/questions.md`**
  Add Q24 (StreamingResponseBody vs ResponseBodyEmitter) and Q25 (Multipart file upload security) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q26, Q27, Q28 to `docs/topics/spring-mvc/questions.md`**
  Add Q26 (Forwarded headers security), Q27 (Custom binary message converters), Q28 (Async request timeout handling) using 7-part format with cross-links to REST API and Security.

- [ ] **Step 4: Append Scenario 29 and Scenario 30 to `docs/topics/spring-mvc/questions.md`**
  Add Scenario 29 (Tomcat thread pool exhaustion from downstream blocking calls) and Scenario 30 (Jackson circular serialization crash) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 06 examples and links**
  Run `./gradlew :modules:06-spring-mvc:compileExamplesJava` and `.venv/bin/mkdocs build --strict`.

- [ ] **Step 6: Commit Module 06 expansion**
  ```bash
  git add modules/06-spring-mvc/ docs/topics/spring-mvc/
  git commit -m "feat(spring-mvc): expand question bank to 30 questions with cross-links"
  ```

---

### Task 2: Module 07 — Spring Transactions Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q24TransactionSynchronizationLifecycleExample.java`
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q25ReadWriteDataSourceRoutingExample.java`
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q26TransactionalOutboxAtomicityExample.java`
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q27SagaVersusTwoPhaseCommitExample.java`
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q28HikariConnectionLeakDetectionExample.java`
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q29NestedPropagationRollbackScenarioExample.java`
  - `modules/07-spring-transactions/src/examples/java/lab/springtransactions/questions/Q30RemoteCallInTransactionScenarioExample.java`
- Modify:
  - `docs/topics/spring-transactions/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.springtransactions.questions.*` and 7 new questions in `docs/topics/spring-transactions/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 07**
  Create `Q24TransactionSynchronizationLifecycleExample.java` (TransactionSynchronizationManager lifecycle hooks), `Q25ReadWriteDataSourceRoutingExample.java` (AbstractRoutingDataSource determining read-only replicas), `Q26TransactionalOutboxAtomicityExample.java` (atomic outbox table write in same transaction), `Q27SagaVersusTwoPhaseCommitExample.java` (Saga compensating actions vs XA 2PC blocking locks), `Q28HikariConnectionLeakDetectionExample.java` (HikariCP leakDetectionThreshold and stack traces), `Q29NestedPropagationRollbackScenarioExample.java` (UnexpectedRollbackException from swallowed rollback-only exception in REQUIRED child), and `Q30RemoteCallInTransactionScenarioExample.java` (slow remote HTTP call holding open DB connection).

- [ ] **Step 2: Append Intermediate Q24 and Q25 to `docs/topics/spring-transactions/questions.md`**
  Add Q24 (TransactionSynchronization execution order) and Q25 (Dynamic read-write replica routing) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q26, Q27, Q28 to `docs/topics/spring-transactions/questions.md`**
  Add Q26 (Transactional outbox atomicity), Q27 (Saga vs 2PC in distributed systems), Q28 (HikariCP connection leak detection) using 7-part format with cross-links to Database/SQL and Messaging.

- [ ] **Step 4: Append Scenario 29 and Scenario 30 to `docs/topics/spring-transactions/questions.md`**
  Add Scenario 29 (UnexpectedRollbackException from caught exception in nested boundary) and Scenario 30 (Connection pool starvation from remote HTTP calls inside @Transactional) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 07 examples and links**
  Run `./gradlew :modules:07-spring-transactions:compileExamplesJava` and `.venv/bin/mkdocs build --strict`.

- [ ] **Step 6: Commit Module 07 expansion**
  ```bash
  git add modules/07-spring-transactions/ docs/topics/spring-transactions/
  git commit -m "feat(spring-transactions): expand question bank to 30 questions with cross-links"
  ```

---

### Task 3: Module 08 — JPA / Hibernate Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q24HibernateActionQueueOrderingExample.java`
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q25SecondLevelCacheRegionsExample.java`
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q26OsivDisabledLazyLoadingExample.java`
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q27JoinFetchPaginationHazardExample.java`
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q28StatelessSessionBatchStreamingExample.java`
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q29PersistenceContextBatchLeakScenarioExample.java`
  - `modules/08-jpa-hibernate/src/examples/java/lab/jpahibernate/questions/Q30InventoryOversellingScenarioExample.java`
- Modify:
  - `docs/topics/jpa-hibernate/questions.md`
  - `docs/topics/jpa-hibernate/practice.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.jpahibernate.questions.*` and formalizes `temporary-practise.md` questions into canonical questions.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 08**
  Create `Q24HibernateActionQueueOrderingExample.java` (ActionQueue flush order: Inserts -> Updates -> Collection removals -> Deletions), `Q25SecondLevelCacheRegionsExample.java` (L2 cache entity/collection regions and query cache stale reads), `Q26OsivDisabledLazyLoadingExample.java` (OSIV disabled LazyInitializationException, transactional service DTO projection / EntityGraph mapping), `Q27JoinFetchPaginationHazardExample.java` (JOIN FETCH with Pageable HHH000104 memory pagination vs two-phase ID paging and @BatchSize), `Q28StatelessSessionBatchStreamingExample.java` (StatelessSession without dirty checking for large dataset streaming), `Q29PersistenceContextBatchLeakScenarioExample.java` (First-level cache memory leak during batch loop resolved via clear/flush), and `Q30InventoryOversellingScenarioExample.java` (optimistic @Version vs pessimistic lock preventing inventory race conditions).

- [ ] **Step 2: Append Intermediate Q24 and Q25 to `docs/topics/jpa-hibernate/questions.md`**
  Add Q24 (ActionQueue execution phases) and Q25 (Second-Level Cache regions) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q26, Q27, Q28 to `docs/topics/jpa-hibernate/questions.md`**
  Add Q26 (OSIV disabled Lazy loading & DTO projections from temporary-practise Q1), Q27 (JOIN FETCH pagination hazards & two-phase ID paging from temporary-practise Q2), Q28 (StatelessSession batch streaming) using 7-part format with cross-links.

- [ ] **Step 4: Append Scenario 29 and Scenario 30 to `docs/topics/jpa-hibernate/questions.md`**
  Add Scenario 29 (First-level cache heap exhaustion in batch jobs) and Scenario 30 (Inventory overselling under concurrent check-then-act) using symptom-first 7-part format.

- [ ] **Step 5: Update `docs/topics/jpa-hibernate/practice.md` with recall cards**
  Add Q2 and Q3 to `practice.md` with Merged from and Canonical links to Q26 and Q27.

- [ ] **Step 6: Verify Module 08 examples and links**
  Run `./gradlew :modules:08-jpa-hibernate:compileExamplesJava` and `.venv/bin/mkdocs build --strict`.

- [ ] **Step 7: Commit Module 08 expansion**
  ```bash
  git add modules/08-jpa-hibernate/ docs/topics/jpa-hibernate/
  git commit -m "feat(jpa-hibernate): expand question bank to 30 questions with cross-links"
  ```

---

### Task 4: Module 09 — Database / SQL Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q24PartialAndExpressionIndexesExample.java`
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q25PostgresCteOptimizationFenceExample.java`
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q26SkipLockedWorkerQueueExample.java`
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q27ExpandAndContractMigrationExample.java`
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q28KeysetPaginationPlannerCostExample.java`
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q29TableAndIndexBloatScenarioExample.java`
  - `modules/09-database-sql/src/examples/java/lab/databasesql/questions/Q30SlowQueryPoolExhaustionScenarioExample.java`
- Modify:
  - `docs/topics/database-sql/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.databasesql.questions.*` and 7 new questions in `docs/topics/database-sql/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 09**
  Create `Q24PartialAndExpressionIndexesExample.java` (partial index and expression index predicates), `Q25PostgresCteOptimizationFenceExample.java` (CTE MATERIALIZED vs NOT MATERIALIZED), `Q26SkipLockedWorkerQueueExample.java` (SELECT FOR UPDATE SKIP LOCKED worker queue), `Q27ExpandAndContractMigrationExample.java` (dual-write zero-downtime schema evolution), `Q28KeysetPaginationPlannerCostExample.java` (seek method vs OFFSET cost on large tables), `Q29TableAndIndexBloatScenarioExample.java` (dead tuple bloat and pg_repack remediation), and `Q30SlowQueryPoolExhaustionScenarioExample.java` (unindexed sequential scan blocking connection pool).

- [ ] **Step 2: Append Intermediate Q24 and Q25 to `docs/topics/database-sql/questions.md`**
  Add Q24 (Partial and Expression indexes) and Q25 (Postgres CTE optimization fences) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q26, Q27, Q28 to `docs/topics/database-sql/questions.md`**
  Add Q26 (SKIP LOCKED distributed worker queues), Q27 (Zero-downtime Expand-Contract migrations), Q28 (Keyset pagination planner cost) using 7-part format with cross-links.

- [ ] **Step 4: Append Scenario 29 and Scenario 30 to `docs/topics/database-sql/questions.md`**
  Add Scenario 29 (Autovacuum starvation and index bloat) and Scenario 30 (Slow analytics queries causing pool exhaustion) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 09 examples and links**
  Run `./gradlew :modules:09-database-sql:compileExamplesJava` and `.venv/bin/mkdocs build --strict`.

- [ ] **Step 6: Commit Module 09 expansion**
  ```bash
  git add modules/09-database-sql/ docs/topics/database-sql/
  git commit -m "feat(database-sql): expand question bank to 30 questions with cross-links"
  ```

---

### Task 5: Module 10 — REST API Question Expansion (Q24–Q30)

**Files:**
- Create:
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q24ProblemDetailExtensionsExample.java`
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q25IdempotencyKeyDeduplicationExample.java`
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q26ConditionalRequestsLostUpdateExample.java`
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q27AsyncLongRunningTaskExample.java`
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q28MassAssignmentDtoPreventionExample.java`
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q29RetryStormPaymentDuplicateScenarioExample.java`
  - `modules/10-rest-api/src/examples/java/lab/restapi/questions/Q30UnboundedQueryOomScenarioExample.java`
- Modify:
  - `docs/topics/rest-api/questions.md`

**Interfaces:**
- Produces: 7 new compilable examples under `lab.restapi.questions.*` and 7 new questions in `docs/topics/rest-api/questions.md`.

- [ ] **Step 1: Create the 7 compilable Java example classes for Module 10**
  Create `Q24ProblemDetailExtensionsExample.java` (RFC 9457 custom properties and error codes), `Q25IdempotencyKeyDeduplicationExample.java` (Idempotency-Key header storage and replay), `Q26ConditionalRequestsLostUpdateExample.java` (If-Match ETag concurrency validation), `Q27AsyncLongRunningTaskExample.java` (202 Accepted status polling pattern), `Q28MassAssignmentDtoPreventionExample.java` (record DTOs blocking internal entity field mutation), `Q29RetryStormPaymentDuplicateScenarioExample.java` (payment gateway client timeout retry storms), and `Q30UnboundedQueryOomScenarioExample.java` (unpaged query parameter exhausting JVM heap).

- [ ] **Step 2: Append Intermediate Q24 and Q25 to `docs/topics/rest-api/questions.md`**
  Add Q24 (RFC 9457 extension properties) and Q25 (Idempotency-Key specification) with snippet inclusions and concept links.

- [ ] **Step 3: Append Senior Q26, Q27, Q28 to `docs/topics/rest-api/questions.md`**
  Add Q26 (ETag lost update prevention), Q27 (Asynchronous 202 Accepted workflows), Q28 (Mass Assignment prevention via DTOs) using 7-part format with cross-links.

- [ ] **Step 4: Append Scenario 29 and Scenario 30 to `docs/topics/rest-api/questions.md`**
  Add Scenario 29 (Payment timeout retry storm and idempotency) and Scenario 30 (Unbounded search parameters triggering OOM) using symptom-first 7-part format.

- [ ] **Step 5: Verify Module 10 examples and links**
  Run `./gradlew :modules:10-rest-api:compileExamplesJava` and `.venv/bin/mkdocs build --strict`.

- [ ] **Step 6: Commit Module 10 expansion**
  ```bash
  git add modules/10-rest-api/ docs/topics/rest-api/
  git commit -m "feat(rest-api): expand question bank to 30 questions with cross-links"
  ```

---

### Task 6: Progress Tracking & Final Batch 2 Verification

**Files:**
- Modify:
  - `docs/progress.md`

- [ ] **Step 1: Update `docs/progress.md`**
  Reflect completion of Batch 2 (Modules 06–10 now at 30 questions each, total 300 questions across Modules 01–10).

- [ ] **Step 2: Run Spotless and full build verification**
  Run `./gradlew spotlessApply`, `./gradlew compileExamples`, and `.venv/bin/mkdocs build --strict`.

- [ ] **Step 3: Commit Batch 2 completion**
  ```bash
  git add docs/progress.md
  git commit -m "docs(progress): update baseline question counts for batch 2 (modules 06-10)"
  ```
