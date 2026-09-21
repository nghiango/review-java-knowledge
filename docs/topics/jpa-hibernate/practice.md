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

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Interview Questions](questions.md)
- [Related: generated `@Id` in `hashCode()`](questions.md#13-why-is-using-a-generated-id-in-hashcode-an-anti-pattern)
- [Core Java: `equals` and `hashCode`](../core-java/questions.md#2-what-contract-connects-equals-and-hashcode)
