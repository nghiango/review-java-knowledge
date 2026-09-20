# Code Review: Remote API Call Inside Transaction

## Background
During high traffic flash sales, the checkout service started reporting database connection timeout errors (`HikariPool-1 - Connection is not available, request timed out after 30000ms`). Active database CPU was low (<15%), but all 50 HikariCP pool connections were constantly in-use.

## Questions to Consider
1. When does Spring acquire and release a database `Connection` in a `@Transactional` method?
2. What happens to the database connection while a remote HTTP call executes?
3. How should transaction boundaries be narrowed so network I/O does not hold database connections?
