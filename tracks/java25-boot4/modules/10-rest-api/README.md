# Java 25 & Spring Boot 4 Track: REST API Delta

This module covers REST API enhancements and modernization in Spring Boot 4.0 / Spring Framework 7 and Java 25:
- **Declarative HTTP Interfaces (`@HttpExchange`)**: Modern declarative HTTP clients in Spring Framework 7, replacing manual `HttpServiceProxyFactory` boilerplate, with strict timeout configuration, virtual thread safety, and seamless RFC 9457 `ProblemDetail` error decoding.
- **Native REST API Versioning**: Spring Framework 7 native version routing across headers, path segments, and media types, coupled with RFC 8594 `Deprecation` and `Sunset` response headers for graceful API lifecycle evolution.
- **Strict Contracts with JSpecify & Jackson 3**: Enforcing non-nullability across REST request and response records via JSpecify `@NullMarked`, ensuring clean API schemas without runtime surprises.

See the canonical track documentation in [`docs/tracks/java25-boot4/rest-api/`](../../../../docs/tracks/java25-boot4/rest-api/).
