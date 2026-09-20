# Code Review: N+1 Query Problem

## Background
`OrderSummaryService` computes total revenue by loading all orders and summing item prices. In production with 10,000 orders, database CPU spiked to 100% and the endpoint took 35 seconds to respond due to thousands of SQL queries executed in rapid succession.

## Questions to Consider
1. What SQL statements are generated when `order.getItems()` is accessed inside the loop?
2. How does Hibernate execute lazy collection loading?
3. How can JPQL `JOIN FETCH`, `@EntityGraph`, or DTO projection reduce this to a single SQL query?
