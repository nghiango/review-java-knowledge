# Code Review — Logging Secrets and PII

## Context

An authentication and billing audit component logs security events and payment transactions. An engineer authored `UserAuthenticationLogger.java` to record user login attempts, session issuance, and payment processing for operational visibility.

Review `UserAuthenticationLogger.java` for data privacy compliance, sensitive credential leakage, and structured logging practices.

## What to look for

- Plaintext exposure of credentials, passwords, tokens, and cryptographic secrets
- Personally Identifiable Information (PII) and Payment Card Industry (PCI-DSS) violations
- Log injection risks from unsanitized string concatenation
- Use of parameterized logging vs eager string formatting
