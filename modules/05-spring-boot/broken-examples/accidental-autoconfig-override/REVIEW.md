# Code Review Target: Accidental Auto-Configuration Override

Review the following configuration and service that define a custom payment client. Evaluate how custom bean declarations interact with Spring Boot auto-configured defaults.

## Files Under Review

- `CustomRestClientConfig.java`
- `ExternalPaymentClient.java`
- `PaymentProcessingService.java`

## Review Objectives

1. Determine whether the custom `@Bean` preserves auto-configured metrics, tracing, and client customizers.
2. Evaluate what happens to framework auto-configuration when a user declares a replacement bean without `@ConditionalOnMissingBean` or customizer callbacks.
3. Identify how custom client configuration should properly extend Spring Boot auto-configuration.
