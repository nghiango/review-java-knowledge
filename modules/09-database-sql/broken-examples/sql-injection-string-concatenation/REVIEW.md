# Code Review: SQL Injection via String Concatenation

## Context
A customer search service filters users dynamically by query parameter and status filter. The SQL statement is assembled using Java string concatenation.

## Code Under Review
- `UserSearchController.java` — Repository method `searchUsers(queryParam, status)`.

## Review Questions
1. How can an attacker exploit `queryParam` to bypass filters or extract unauthorized customer records?
2. What impact does raw dynamic SQL string construction have on database `PreparedStatement` plan caching?
3. How does Spring's `JdbcClient` / `NamedParameterJdbcTemplate` provide parameterized safety against SQL injection?
