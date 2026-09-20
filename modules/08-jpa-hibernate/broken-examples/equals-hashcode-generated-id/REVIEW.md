# Code Review: Equals and HashCode on Generated ID

## Background
`UserAccount` defines `equals()` and `hashCode()` based on `@GeneratedValue Long id`. In unit tests and domain workflows where entities are added to a `Set` before saving, calling `set.contains(savedEntity)` returns `false` and duplicate entities are added.

## Questions to Consider
1. What value does `@GeneratedValue Long id` have before an entity is persisted to the database?
2. How does `HashSet` locate buckets using `hashCode()`, and what happens when an object's `hashCode()` mutates after insertion?
3. How can business natural keys (e.g. unique `email`) or immutable generated UUIDs provide stable identity across transient and managed states?
