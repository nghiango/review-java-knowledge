# Code Review / Retrospective Review: Incident 4082 Post-Mortem

## Scenario
Following a high-severity production outage that took down the payment checkout pipeline for 42 minutes, the QA & Release Manager published `post-mortem-incident-4082.md`.

The report singles out a junior engineer ("Alex"), characterizes the cause as human carelessness, and proposes punitive and ineffective action items ("remind developers to be more careful").

As a Senior / Staff Engineer, you are responsible for establishing a resilient, psychologically safe engineering culture and ensuring post-mortems eliminate systemic hazards.

## Review Objectives
1. Critique the report's adherence to John Allspaw's principles of **Blameless Post-Mortems** and Human Factors engineering.
2. Identify the systemic latent conditions (the "Second Story") that permitted a junior engineer to execute an unindexed migration directly on a production database on a Friday afternoon without automated safeguards.
3. Rewrite the post-mortem into an exemplary, blameless retrospective featuring a high-resolution timeline, 5-Whys systemic analysis, telemetry metrics, and actionable engineering controls (Poka-Yoke / automated guardrails).
