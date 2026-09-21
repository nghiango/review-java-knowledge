# Code Review Target: ThreadLocal Context Loss in Async Virtual Workers

## Overview
`PaymentOrchestrator` stores transaction correlation IDs in a `ThreadLocal` during payment processing and dispatches follow-up callbacks onto asynchronous virtual threads.

## Code Under Review
Review `PaymentOrchestrator.java`.

## Questions for the Reviewer
1. What happens when asynchronous virtual workers try to read from `CURRENT_TX_CORRELATION`?
2. What are the memory leak and thread pollution risks of omitting `CURRENT_TX_CORRELATION.remove()`?
3. In Java 25, how does `ScopedValue` provide safe context inheritance into asynchronous virtual tasks without manual cleanup?
