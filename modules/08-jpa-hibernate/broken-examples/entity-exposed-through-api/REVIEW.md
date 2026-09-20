# Code Review: Entity Exposed Through API

## Context
A REST endpoint manages user profiles. The controller accepts JPA entities directly in request payloads and returns JPA entities directly in HTTP response bodies.

## Code Under Review
- `UserProfile.java` — JPA entity containing sensitive fields (`passwordHash`, `admin`).
- `UserProfileController.java` — REST controller binding `UserProfile` in `@RequestBody` and returning `UserProfile` in responses.

## Review Questions
1. What security vulnerabilities (e.g. Mass Assignment, Information Disclosure) are introduced by binding and returning JPA entities directly?
2. How does Hibernate's automatic dirty checking interact with directly bound JPA entities in a transactional service/controller?
3. How should DTOs (Data Transfer Objects) and explicit mappings (or Java 21 records) be used to protect the domain model and presentation contract?
