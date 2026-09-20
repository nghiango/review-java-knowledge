# Code Review Target: God Controller with Business Logic

Review the following checkout controller. Identify architectural, separation of concerns, and security flaws.

## Files Under Review

- `OrderRequest.java`
- `CheckoutController.java`

## Review Objectives

1. Evaluate how responsibilities (HTTP parsing, validation, pricing algorithms, persistence, payments, notifications) are distributed.
2. Determine how this code can be tested in isolation (e.g. unit testing pricing logic without HTTP or SQL).
3. Check for SQL injection vulnerabilities and tight coupling.
