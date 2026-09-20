# Code Review Target: Scattered Value Configuration

Review the following classes that extract configuration properties directly using `@Value`. Identify maintainability, validation, and design issues.

## Files Under Review

- `BillingConfigConsumer.java`
- `PaymentNotificationService.java`

## Review Objectives

1. Identify how property keys are distributed across components.
2. Evaluate what happens if an invalid, negative, or blank property is supplied in configuration.
3. Check for duplicated default values and lack of type-safe grouping.
