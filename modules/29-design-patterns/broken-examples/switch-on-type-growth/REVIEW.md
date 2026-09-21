# Code Review: Procedural Switch-on-Type Growth

## Context
You are reviewing a payment processing service handling transactions across multiple payment rails (`CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, `CRYPTO`, `BANK_TRANSFER`).

The business is rapidly onboarding new payment options (Apple Pay, Google Pay, Buy-Now-Pay-Later/Klarna).

## Your Task
Inspect `PaymentProcessor.java`, `PaymentRequest.java`, and `PaymentType.java`.
Identify maintainability and testability concerns:
- What happens when a new payment rail is added?
- How is error handling and validation decoupled?
- How can this code be refactored to follow object-oriented design patterns?
