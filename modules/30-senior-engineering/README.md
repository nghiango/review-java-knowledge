# Module 30 — Senior Engineering

This is a **doc module** covering technical leadership, engineering decision-making, and organizational excellence for Senior and Staff Backend Engineers:

- **Architecture Decision Records (ADRs) & RFCs**: Capturing context, evaluating options, analyzing trade-offs, and documenting positive/negative consequences.
- **Incident Management & Blameless Post-Mortems**: Incident Commander (IC) operational dynamics, blameless culture, human factors (Swiss cheese model), and high-leverage systemic remediation.
- **Code Review as Mentorship**: Conventional Comments, separating architectural blockers from minor nits, psychological safety, and coaching code review habits.
- **Technical Debt Management**: Debt taxonomy (Prudent vs Reckless), technical debt quantification, principal vs interest, and negotiation with product stakeholders.
- **Engineering Estimation & Delivery**: Cone of Uncertainty, PERT three-point estimates, managing scope creep, and balancing velocity with maintainability.
- **Disagreements & Mentoring**: "Disagree and commit", resolving technical impasses via spikes/evidence, situational coaching, and delegating ownership.

The canonical prose, operational runbooks, interview Q&A, and incident templates live in the documentation:

👉 **[Senior Engineering Documentation](../../docs/topics/senior-engineering/index.md)**

## Broken Review Examples

This module provides 3 realistic broken artifacts under `broken-examples/`:

1. `poorly-written-adr/` — A dogmatic, one-sentence Architecture Decision Record proposing a complex microservices migration with zero business context, no alternatives evaluated, and no consequences analyzed.
2. `blame-oriented-post-mortem/` — A toxic, blame-focused incident retrospective that targets an individual engineer, attributes an outage to "human carelessness", and proposes superficial action items.
3. `unhelpful-code-review-comments/` — A destructive PR review filled with sarcastic nitpicking, bikeshedding variable names, and condescension, while missing a critical SQL injection vulnerability and a concurrency race condition.
