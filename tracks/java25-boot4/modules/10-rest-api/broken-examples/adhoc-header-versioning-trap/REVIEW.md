# Code Review: Ad-Hoc Header Versioning & Missing Lifecycle Headers

## Context
A pull request has been submitted to support version 2 of the Order REST API while maintaining backward compatibility with version 1 clients. The engineer used `@RequestHeader(value = "X-API-Version", required = false)` directly inside the controller method and performed imperative branching to return either `LegacyOrderDto` or `ModernOrderRepresentation`.

## Files Under Review
- `OrderResourceController.java`
- `LegacyOrderDto.java`

## Review Questions
1. How does the controller handle requests when a client requests an unsupported or malformed API version? What HTTP status and body format does the client receive?
2. Does the controller communicate API lifecycle state (e.g. deprecation, sunset dates) to v1 clients as mandated by REST standards (RFC 8594)?
3. How does embedding version dispatch logic directly inside the resource controller method impact endpoint documentation, maintainability, and content negotiation?
4. How do the data contracts of `LegacyOrderDto` and `ModernOrderRepresentation` align with modern Java 25 / Spring Boot 4 immutable DTO and nullability standards?
