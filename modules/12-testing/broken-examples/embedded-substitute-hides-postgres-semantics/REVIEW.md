# Review: In-memory repository that hides PostgreSQL semantics

## Context

A team added `AccountService`, which registers accounts and reads their balance, together with an
`InMemoryAccountRepository` so the account tests could run without Docker. The suite is fast, green
in CI and is treated as the safety net for registration — but the repository the tests exercise is a
map keyed by a lower-cased email, and the service leans on that key for both uniqueness and lookup.

The store the service ships against is PostgreSQL, where the unique index on `email` and the
`findByEmail` derived query both compare case-sensitively, and where row order is only defined when a
`Sort` asks for it.

## Target Files

- [`InMemoryAccountRepository.java`](InMemoryAccountRepository.java)
- [`AccountService.java`](AccountService.java)
- [`AccountServiceTest.java`](AccountServiceTest.java)

## Task

Review the three files as if they were a pull request. Identify every problem with the substitute and
with the tests built on it. Consider:

- which semantics of the real store the map reproduces, and which it silently replaces
- what the `email` unique index and the `findByEmail` query do on PostgreSQL, and whether the map
  agrees
- where the case-insensitive matching comes from, and whether it is a rule the application states or
  a property of the substitute
- what `findAll` guarantees about row order, and what the tests assume
- what happens when two accounts share an address, and which layer is supposed to reject that
- what a defect in registration or lookup would have to look like in order to fail this suite
- how much of each assertion is about `AccountService` and how much is about the map

Write your findings down before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:12-testing:compileBrokenExamples
```
