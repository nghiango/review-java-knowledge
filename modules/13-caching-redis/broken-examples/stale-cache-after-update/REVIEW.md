# Code Review — Stale Cache After Entity Mutation

## Context

A product catalog service caches product lookup results in the Spring Cache abstraction using `@Cacheable`. The service also provides mutation operations to update prices and delete products.

Review `ProductService.java` for data consistency issues, cache invalidation defects, and cache lifecycle mismatches.

## What to look for

- Cache synchronization on write/mutation operations
- Cache invalidation on entity deletion
- Handling of `Optional` return types in Spring Cache
- Cache key scoping and consistency
