# Senior Engineering Code Review Lab

Practice senior engineering review skills across realistic flawed artifacts: a dogmatic Architecture Decision Record (ADR), a blame-oriented production incident post-mortem, and destructive pull request review comments.

---

## Exercise 1: Dogmatic & Incomplete Architecture Decision Record (ADR)

### Scenario
A tech lead has submitted an ADR proposing to rewrite a Spring Boot monolith into 12 microservices. Review the document for architectural rigor, missing context, implicit assumptions, and failure to evaluate trade-offs.

### Review Target

--8<-- "modules/30-senior-engineering/broken-examples/poorly-written-adr/adr-004-use-microservices.md"

??? question "Prompt & Guidance"
    - Does this ADR state quantifiable business or technical drivers (e.g. QPS, latency, failure rates)?
    - Does it evaluate viable alternatives (e.g. Modular Monolith with Spring Modulith, targeted domain extraction)?
    - Does it acknowledge the negative operational consequences of distributed systems (distributed transactions, network latency, distributed tracing)?
    - Does it account for organizational size (8 backend engineers maintaining 12 services)?

??? example "Review Reveal & Findings"
    - **Vague Problem Definition**: "Too slow" and "getting too big" are subjective claims. What are the actual bottlenecks? Database locks? CI build times? Memory saturation?
    - **Violation of Conway's Law**: 8 backend engineers cannot sustainably support 12 microservices, 12 CI/CD pipelines, and 12 on-call rotations without severe operational burnout.
    - **Zero Alternatives Evaluated**: Jumps directly to a big-bang rewrite without considering a Modular Monolith or the Strangler Fig pattern.
    - **One-Sided Positive Bias**: Omits all negative trade-offs of microservices (network partitions, eventual consistency, distributed Sagas).
    - See the full analysis in [Solutions: ADR 004](solutions.md#1-poorly-written-architecture-decision-record-adr).

---

## Exercise 2: Blame-Oriented Production Incident Post-Mortem

### Scenario
Following a 42-minute production checkout outage, the QA & Release Manager published a post-mortem attributing the crash to a junior developer who ran an unindexed SQL query. Critique the report from a human factors and blameless engineering perspective.

### Review Target

--8<-- "modules/30-senior-engineering/broken-examples/blame-oriented-post-mortem/post-mortem-incident-4082.md"

??? question "Prompt & Guidance"
    - Does the post-mortem name individuals and assign personal blame?
    - What are the latent systemic conditions (the "Second Story") that allowed an unindexed query to freeze production?
    - How effective are the proposed action items ("retrain Alex", "remind developers to be careful")?
    - How was the incident detected, and what does that say about observability?

??? example "Review Reveal & Findings"
    - **Destruction of Psychological Safety**: Naming "Alex" and calling them "negligent" guarantees engineers will hide future mistakes and delay incident disclosures.
    - **Superficial "Human Error" Root Cause**: Fails to ask *why* developers have direct production write access, *why* staging lacks representative data volume, and *why* PostgreSQL had no `statement_timeout`.
    - **Low-Leverage Action Items**: "Be more careful" and "write in Slack" are administrative band-aids with a 100% failure rate over time.
    - **Missing Telemetry**: Detection occurred via customer Twitter complaints rather than automated SLO burn-rate alerts.
    - See the full analysis in [Solutions: Incident 4082](solutions.md#2-blame-oriented-incident-post-mortem).

---

## Exercise 3: Unhelpful & Destructive Code Review Comments

### Scenario
A senior engineer submitted review comments on a junior engineer's pull request adding a customer search API. Review both the code diff and the submitted comments.

### Review Target

--8<-- "modules/30-senior-engineering/broken-examples/unhelpful-code-review-comments/pr-review-comments.md"

??? question "Prompt & Guidance"
    - What critical security and reliability bugs exist in the code diff that the reviewer completely overlooked?
    - What tone and communication anti-patterns did the reviewer exhibit?
    - How does bikeshedding over variable names and indentation undermine code review effectiveness?
    - How would you rewrite these comments using the Conventional Comments specification?

??? example "Review Reveal & Findings"
    - **Missed Catastrophic Vulnerabilities**:
      - *Critical SQL Injection*: `"SELECT * FROM customers WHERE email = '" + queryParam + "'"` enables trivial data exfiltration.
      - *Connection Leak*: Unclosed `Connection`, `Statement`, and `ResultSet` will exhaust HikariCP within minutes.
      - *Concurrency Race Condition*: Shared `HashMap` across HTTP threads causes data corruption or infinite CPU spin loops under load.
    - **Bikeshedding Anti-Pattern**: Reviewer spent 100% of their effort on indentation and subjective naming (`CustomerSearchQueryFacadeHandler`, `inputParameterString`).
    - **Hostile Tone**: Condescending phrasing ("Did you even bother running the formatter before pinging me?", "looks like amateur code") destroys trust and mentorship.
    - See the full analysis in [Solutions: PR Review Comments](solutions.md#3-unhelpful-code-review-comments).
