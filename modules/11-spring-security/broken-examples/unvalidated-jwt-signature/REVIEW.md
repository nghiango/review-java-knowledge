# Code Review: Unvalidated JWT Signature & Expiration

## Context
A stateless REST API uses a custom `OncePerRequestFilter` to authenticate incoming requests via JSON Web Tokens (JWT) in the `Authorization: Bearer <token>` header.

## Target Files
- [`JwtClaims.java`](JwtClaims.java)
- [`JwtTokenParser.java`](JwtTokenParser.java)
- [`InsecureJwtFilter.java`](InsecureJwtFilter.java)

## Task
Review `JwtTokenParser.java` and `InsecureJwtFilter.java`. Identify critical cryptographic vulnerabilities, lack of signature validation, `alg: none` susceptibility, and missing token expiration checks.
