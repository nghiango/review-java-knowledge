# Java 25 & Spring Boot 4 Track: Spring Security Delta

This module covers the Spring Security 7 modernization in Spring Boot 4.0 / Spring Framework 7 and Java 25:
- **Spring Security 7 Lambda-Only DSL**: Complete removal of legacy `and()` chaining and deprecated configuration builders in favor of pure lambda-based `Customizer` configurations.
- **Virtual Thread Security Context Propagation**: Safe concurrency models eliminating memory retention and context leakage associated with `MODE_INHERITABLETHREADLOCAL`, utilizing Java 25 `ScopedValue` for zero-overhead, strictly scoped authentication propagation.
- **Modern Authorization Managers & Customizers**: Explicit request matching, stateless session management, and granular method security.

See the canonical track documentation in [`docs/tracks/java25-boot4/spring-security/`](../../../../docs/tracks/java25-boot4/spring-security/).
