# Senior Engineering — Practice Q&A

Practice from interview-prep §14, §16, and §19.12. These are frameworks for truthful answers,
not scripts claiming the supplied case study's work or measurements as your personal experience.

### Q1. How do you explain a difficult technical decision using STAR?

??? question "Reveal answer"
    - **Explain the constraint, your actual contribution, alternatives, and observed result.**
    - **Mechanism:** Situation/Task establish stakes; Action explains evidence and choice; Result reports outcomes and residual risk. An ADR captures the assumptions for later review.
    - **Failure:** presenting textbook outbox benefits as personal production results undermines credibility; a technology inventory does not demonstrate reasoning.
    - **Fix:** use a real decision, compare direct publication/outbox/CDC where relevant, explain the rejected alternatives, and name the accepted cost such as polling and duplicate handling.
    - **Trade-off:** include uncertainty and what would trigger reconsideration rather than defending the choice universally.
    - **Follow-up:** What evidence would make you reverse the decision today?
    - **Takeaway:** Ownership includes the cost you knowingly accepted.

### Q2. How do you tell a credible story about finding a production-class defect?

??? question "Reveal answer"
    - **Show evidence from symptom through root cause, fix, and recurrence prevention.**
    - **Mechanism:** a broken metric contract, prematurely released claim lock, or unprotected compensation publication each has a concrete failure interleaving.
    - **Failure:** implying a review finding caused a real outage when it did not fabricates impact. Repairing the symptom without a regression test leaves the failure window open.
    - **Fix:** distinguish observed from potential impact, explain your contribution, reproduce safely, fix the boundary, and add a test or operational guardrail.
    - **Trade-off:** discuss why the defect escaped detection without blame and what the remedy costs.
    - **Follow-up:** How did you verify business data was correct after recovery?
    - **Takeaway:** A strong incident story is falsifiable and honest.

### Q3. How do you demonstrate that a system meets its SLOs?

??? question "Reveal answer"
    - **Connect explicit SLIs to reproducible qualification and sustained operational evidence.**
    - **Mechanism:** define workload, good events, measurement window, and target; load/failure tests qualify an envelope while burn-rate alerts observe real use.
    - **Failure:** quoting one cached endpoint's throughput does not prove all APIs, 10k users, or availability. A target is not an achieved result.
    - **Fix:** retain exact configuration, offered load, errors, percentiles, saturation, and reconciliation results; state limits and test duration.
    - **Trade-off:** stronger qualification costs environment time and maintenance; prioritize highest-risk journeys rather than inflating benchmark breadth.
    - **Follow-up:** Which scenario remains unqualified and why?
    - **Takeaway:** Separate aspirations, experiments, and production evidence.

### Q4. How do you handle disagreement about architecture?

??? question "Reveal answer"
    - **Align on constraints and decision ownership, then test competing assumptions.**
    - **Mechanism:** an ADR records options, expected outcomes, evidence, and a review trigger; a bounded experiment can settle measurable questions.
    - **Failure:** debating taste indefinitely delays delivery; 'disagree and commit' does not justify ignoring material security or correctness risks.
    - **Fix:** make evaluation criteria explicit, invite counterevidence, escalate unresolved high-impact risks through the agreed owner, and document the chosen trade-off respectfully.
    - **Trade-off:** reversible choices deserve lighter process than irreversible data or cross-team contracts; avoid requiring consensus on every detail.
    - **Follow-up:** What if the experiment is inconclusive but the release must proceed?
    - **Takeaway:** Resolve uncertainty and ownership, not personalities.

### Q5. How do you mentor and raise engineering standards without becoming a bottleneck?

??? question "Reveal answer"
    - **Make reasoning reusable and transfer ownership instead of rewriting everyone's code.**
    - **Mechanism:** architecture fitness tests, focused examples, runbooks, review rubrics, and paired investigation turn repeated feedback into shared capability.
    - **Failure:** broad mandatory rules without rationale create ritual compliance; all decisions flowing through one senior engineer limit team throughput.
    - **Fix:** distinguish blockers from suggestions, explain invariants, automate mechanical checks, and give gradually larger responsibilities with feedback.
    - **Trade-off:** guardrails need maintenance and escape paths for justified exceptions; measure autonomy and reduced recurrence, not just document counts.
    - **Follow-up:** How do you know the team can operate the system without you?
    - **Takeaway:** Senior leverage is increased capability in others.

### Q6. How do you prioritize known gaps and explain what you would change next?

??? question "Reveal answer"
    - **Rank gaps by correctness impact, exposure, detectability, and repair cost.**
    - **Mechanism:** group the case-study gaps into lost intent/commit gaps, unauthorized access, overload, stale reads, and blind telemetry; assign owners and validation criteria.
    - **Failure:** fixing an LRU label before an unprotected payment or compensation path optimizes polish over risk. A simulated inventory subsystem is not qualified stock handling.
    - **Fix:** close authorization and durable-effect gaps first, add detection, then address capacity and efficiency according to evidence. Document consciously deferred work and review triggers.
    - **Trade-off:** one aggregate topic, globally coordinated quotas, and tracing exporters have real migration/operations costs; propose a staged change rather than a rewrite.
    - **Follow-up:** Which risk would you accept for the next release, with what mitigation?
    - **Takeaway:** A senior roadmap explains risk reduction, not just more features.

### Q7. What makes a blameless postmortem produce lasting improvements?

??? question "Reveal answer"
    - **Turn a verified timeline and contributing conditions into owned, testable prevention.**
    - **Mechanism:** record impact, detection gap, mitigation, root causes, what helped, and where luck limited damage. Include technical and organizational conditions.
    - **Failure:** 'be more careful' and ownerless tasks do not change the system; a single five-whys chain may hide multiple contributing factors.
    - **Fix:** assign owners and review dates, prioritize automated guardrails, and validate completion by reproducing the failure or exercising the alert/runbook.
    - **Trade-off:** not every incident merits a large process, but recurring low-severity failures can reveal systemic risk. Protect sensitive data in the report.
    - **Follow-up:** How do you know the action item actually reduced incident risk?
    - **Takeaway:** Blameless does not mean actionless.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Solutions](solutions.md)
- [Incident investigation practice](../observability/practice.md)
- [Architecture practice](../architecture/practice.md)
