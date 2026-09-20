# Code Review Target: Missing Input Validation

Review the following request DTO, controller, and service handling user registration. Identify data integrity, validation, and API contract flaws.

## Files Under Review

- `CreateUserRequest.java`
- `UserController.java`
- `UserService.java`

## Review Objectives

1. Determine whether payload fields are validated before reaching the business service.
2. Evaluate what happens if an invalid email, blank username, or negative balance is submitted.
3. Check Spring MVC validation annotation usage on the controller method parameter.
