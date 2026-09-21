# Code Review: Decorator Order Bug

## Context
You are reviewing the decorator pipeline composition for `OrderQueryService`.
The requirements state that confidential orders (e.g. `ORD-CONFIDENTIAL-99`) must only be accessible to users with the `ADMIN` role.
To improve read throughput, caching was introduced via `CachingOrderQueryDecorator`.

## Your Task
Inspect `OrderQueryServiceFactory.java`, `AuthorizingOrderQueryDecorator.java`, and `CachingOrderQueryDecorator.java`.
Analyze the execution order:
- What happens when an `ADMIN` accesses `ORD-CONFIDENTIAL-99`?
- What happens when an unauthenticated or `GUEST` user subsequently accesses `ORD-CONFIDENTIAL-99`?
- Identify the vulnerability and how to guarantee correct decorator ordering.
