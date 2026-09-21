# Code Review: Legacy Spring Security DSL Chaining & Matcher Ordering

## Context
A pull request modernizes the administrative portal backend. The author configured the HTTP security filter chain to secure `/admin/**` endpoints and permit `/public/**` access.

## Files Under Review
- `LegacySecurityConfig.java`
- `AdminUserController.java`

## Review Questions
1. How does the Spring Security filter chain configuration style in `filterChain` align with Spring Security 7 and Spring Boot 4 requirements? What happens to `and()` method chaining?
2. How does the matcher configuration handle incoming requests that match multiple rules? Could any request unintentionally bypass authorization?
3. How should modern Spring Security 7 configurations express CSRF, session management, and authorization rules using the lambda DSL and `Customizer` interfaces?
