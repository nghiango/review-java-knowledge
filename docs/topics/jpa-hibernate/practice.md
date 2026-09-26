# JPA / Hibernate — Practice Q&A

Quick-recall questions. Answers are collapsed; think before revealing.

### Q1. Two `EntityManager`s load the same customer by id. What do `==` and `equals()` return, and why does that hurt `Set<Customer>`? (`Customer` overrides neither `equals()` nor `hashCode()`.)

```java
Customer c1 = entityManager1.find(Customer.class, 123L);
Customer c2 = entityManager2.find(Customer.class, 123L);

System.out.println(c1 == c2);
System.out.println(c1.equals(c2));
```

Also relevant: [Core Java](../core-java/index.md)

??? question "Reveal answer"
    - **Both `==` and `equals()` return `false` — each persistence context holds its own instance.**
    - Every `EntityManager` keeps its own **first-level cache** (an identity map), so the same row becomes **two objects**.
    - `==` compares **references**, and `equals()` is not overridden, so it compares references too → **both `false`**.
    - A `Set<Customer>` decides membership with `equals()`/`hashCode()`, so the same customer counts as **two elements** — duplicates, and `contains()`/`remove()` silently miss.
    - **Fix:** base `equals()`/`hashCode()` on a **stable business key** (never the generated id), or compare **ids** across transaction boundaries.
    - **Takeaway:** One row, one object — but only inside **one** persistence context.

### Q2. With OSIV disabled (`open-in-view: false`), an endpoint calls `order.getItems()` in the controller and crashes. Would `@Transactional` on `orderService.getOrder(id)` fix it, and what are two production solutions?

```java
@GetMapping("/orders/{id}")
public OrderResponse getOrder(@PathVariable Long id) {
    Order order = orderService.getOrder(id);
    return new OrderResponse(order.getId(), order.getItems().stream().map(OrderItem::getName).toList());
}
```

Merged from: Question 1 (temporary-practise.md)  
Canonical: [Q26 — OSIV disabled lazy loading & DTO projections](questions.md#26-why-does-accessing-lazy-associations-fail-outside-transactions-when-osiv-is-disabled-and-how-do-you-design-read-only-queries-with-dto-projections-or-entitygraphs)  
Also relevant: [Spring Transactions](../spring-transactions/index.md)

??? question "Reveal answer"
    - **No, adding `@Transactional` to `getOrder()` does not fix it — the transaction terminates and the persistence context closes before the controller accesses items.**
    - With OSIV disabled, the entity is detached the moment the service returns.
    - Accessing `order.getItems()` outside the transaction throws `LazyInitializationException`.
    - Adding `@Transactional` only to `getOrder()` ends the session before the view/controller layer executes.
    - **Fix 1:** Perform entity access and DTO mapping inside the transactional service boundary using `JOIN FETCH` or `@EntityGraph`.
    - **Fix 2:** Use direct constructor DTO projection (`record OrderResponse(...)`) in the query to avoid loading entity state.
    - **Takeaway:** Never let entities escape transactional boundaries when OSIV is disabled; map to DTOs inside the service.

### Q3. Why is combining `JOIN FETCH` on a `@OneToMany` collection with `Pageable` problematic, and how do you redesign it?

```java
@Query("""
    SELECT o
    FROM Order o
    JOIN FETCH o.items
""")
Page<Order> findAllWithItems(Pageable pageable);
```

Merged from: Question 2 (temporary-practise.md)  
Canonical: [Q27 — JOIN FETCH pagination hazard & two-phase ID paging](questions.md#27-why-is-combining-join-fetch-on-a-to-many-association-with-pagination-dangerous-and-how-do-you-redesign-it-using-two-phase-id-pagination-or-batch-fetching)  
Also relevant: [Database / SQL](../database-sql/index.md)

??? question "Reveal answer"
    - **It forces Hibernate to perform in-memory pagination (`HHH000104`), loading all matching rows into the JVM heap and risking `OutOfMemoryError`.**
    - A to-many fetch join multiplies parent entity rows by the number of child items (e.g. 100 orders with 10 items = 1,000 SQL rows).
    - Applying database `LIMIT` and `OFFSET` directly would slice across child items, returning incomplete collections and incorrect parent page counts.
    - To prevent corrupt data, Hibernate fetches the entire unpaged result set into memory and applies pagination in RAM.
    - **Fix 1:** Two-phase ID pagination: first page the root Order IDs (`SELECT o.id FROM Order o ORDER BY o.id DESC`), then fetch orders and items via `WHERE o.id IN (:ids)`.
    - **Fix 2:** Use batch fetching (`@BatchSize` or `hibernate.default_batch_fetch_size: 20`) without `JOIN FETCH` on the pageable query.
    - **Takeaway:** Never fetch-join to-many collections in pageable queries; paginate IDs first, then batch-fetch children.

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Interview Questions](questions.md)
- [Related: generated `@Id` in `hashCode()`](questions.md#13-why-is-using-a-generated-id-in-hashcode-an-anti-pattern)
- [Core Java: `equals` and `hashCode`](../core-java/questions.md#2-what-contract-connects-equals-and-hashcode)
