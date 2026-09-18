# Security Issues

Authentication, authorization, injection, secret handling and unsafe input boundaries.

## Entries

### Stale authenticated user on reused thread

**Type:** Security issue · **Severity:** High · **Difficulty:** Senior

ThreadLocal security or request context that is not cleared can make anonymous work observe the
previous authenticated user on a reused executor thread. Scope the context lexically and remove it
at the boundary.

**Appears in:** `modules/02-jvm/broken-examples/threadlocal-pool-leak`

## Related

- [Issue catalogue](index.md)
