# Code Review: ADR 004 — Microservices Migration

## Scenario
A tech lead on your team has submitted `adr-004-use-microservices.md` as an Architecture Decision Record to justify a multi-quarter engineering initiative to rewrite the core monolithic application into 12 microservices.

Your engineering team consists of 8 backend engineers, 2 mobile engineers, 2 frontend engineers, and 1 DevOps engineer. The current monolith processes 350 requests/second at peak with a p99 latency of 120ms.

## Review Objectives
1. Evaluate whether the ADR meets standard industry criteria for an Architecture Decision Record (such as the Michael Nygard format).
2. Identify missing context, unspoken assumptions, lack of alternatives, and unaddressed operational costs/consequences.
3. Formulate constructive feedback and rewrite the ADR to provide a balanced, evidence-based architectural decision.
