---
type: code
topic: rest-api
---

# Module 10: REST API

RESTful architectural principles, HTTP methods safety and idempotency, semantic HTTP status codes, RFC 9457 Problem Details (`ProblemDetail`), HTTP caching (`Cache-Control`, `ETag`), optimistic concurrency (`If-Match`), pagination envelopes, API versioning strategies, distributed idempotency keys, and OpenAPI integration.

## Canonical Documentation

Prose, theory, interview questions, architecture diagrams, and exercises are authored in the docs site:
[REST API Topic Documentation](../../docs/topics/rest-api/index.md)

## Layout

- `broken-examples/`: Clean review targets with realistic API design flaws, state-mutating GETs, mass assignment, and unhandled idempotency.
- `src/main/java/lab/restapi/`: Correct production implementations using Spring Web 6.2, RFC 9457 `ProblemDetail`, record DTOs, and idempotency validators.
- `src/examples/java/lab/restapi/questions/`: Dedicated compilable question examples with trailing evaluation result comments (`Q01` through `Q23`).
- `src/test/java/lab/restapi/`: Fast unit test suites.
