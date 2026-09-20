# Code Review Target: CORS Wildcard with Credentials

Review the following CORS configuration and controller. Identify security vulnerabilities and browser specification violations.

## Files Under Review

- `AccountController.java`
- `WebCorsConfig.java`

## Review Objectives

1. Determine whether `allowCredentials(true)` is compatible with `allowedOrigins("*")`.
2. Evaluate what happens when browsers evaluate Cross-Origin requests containing cookies or authorization headers.
3. Identify how trusted origins should be configured using `allowedOriginPatterns` or explicit domain whitelists.
