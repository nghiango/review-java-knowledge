# Code Review — Infinite Retry Loop

## Context

An e-commerce order service executes payment settlement against an external payment processor gateway. The developer wrote a loop to handle transient network blips and ensure every order gets charged.

Review `OrderPaymentService.java` for resilience, resource exhaustion, availability, and error handling issues.

## What to look for

- Loop termination conditions and retry bounds
- Backoff, sleep, or delay between attempts
- Distinction between transient failures and non-retryable errors
- Thread starvation and resource consumption under downstream outages
