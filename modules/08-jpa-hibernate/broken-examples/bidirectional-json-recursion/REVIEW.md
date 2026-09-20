# Code Review: Bidirectional JSON Serialization Recursion

## Background
`BookCatalogController` serializes `Author` and `Book` entities directly into JSON responses using Jackson. In production, calling `getAuthorAsJson` threw `StackOverflowError` (`Direct self-reference leading to cycle`).

## Questions to Consider
1. How does Jackson's `ObjectMapper` navigate bidirectional object references during JSON serialization?
2. What annotations (`@JsonIgnore`, `@JsonManagedReference`, `@JsonBackReference`) or DTO projections prevent recursive cycles?
3. Why should JPA entities never be serialized directly in REST responses?
