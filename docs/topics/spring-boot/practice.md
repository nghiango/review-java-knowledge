# Spring Boot — Practice Q&A

Senior-depth practice from interview-prep §4. This lab's baseline is Java 21 / Boot 3.5;
the source case study's Boot 4 / Jackson 3 details are not baseline facts.

### Q1. What does Boot add to Spring, and what remains your responsibility?

??? question "Reveal answer"
    - **Boot supplies opinionated wiring and operations support, not application correctness.**
    - **Mechanism:** starters align dependencies; auto-configuration conditionally creates beans; embedded servers, external configuration, and Actuator simplify packaging and operation.
    - **Failure:** assuming defaults cover timeouts, authorization, or custom connection pools leaves production gaps. Version upgrades can change defaults and serialization behavior.
    - **Fix:** inspect effective dependencies and configuration, test critical defaults, and document intentional overrides. Use the track docs for newer-stack deltas.
    - **Trade-off:** convention reduces setup but makes understanding conditional configuration essential during customization.
    - **Follow-up:** Which assumptions would you re-qualify before a Boot major upgrade?
    - **Takeaway:** Auto-configuration removes boilerplate, not accountability.

### Q2. Why can custom DataSources silently bypass expected Hikari properties?

??? question "Reveal answer"
    - **Properties affect beans only when the relevant binding and configuration actually run.**
    - **Mechanism:** imports load conditional auto-configurations; conditions inspect classes, properties, and existing bean definitions. User beans often cause specific auto-configurations to back off.
    - **Failure:** YAML contains a leak-detection threshold but manually constructed pools never bind it. Multiple candidates can select the wrong transaction manager or metrics source.
    - **Diagnosis/Fix:** inspect the condition report with `--debug`, bean types, and actual pool configuration; explicitly bind typed settings and test each named pool.
    - **Trade-off:** custom routing offers control but transfers validation, lifecycle, observability, and property-binding responsibility to the application.
    - **Follow-up:** How would a context test prove both back-off and correct effective values?
    - **Takeaway:** Verify runtime bean configuration, not merely configuration files.

### Q3. How should configuration be validated and overridden safely?

??? question "Reveal answer"
    - **Bind coherent typed properties and fail startup for unsafe combinations.**
    - **Mechanism:** `@ConfigurationProperties` binds external sources according to precedence; validation checks required values and ranges, while cross-field constraints enforce relationships.
    - **Failure:** a valid positive timeout can still exceed the request deadline; profile overrides can introduce permissive security or embedded secret defaults.
    - **Fix:** validate budgets and pool bounds together, test precedence, and obtain secrets through approved runtime mechanisms without printing values in diagnostics.
    - **Trade-off:** fail-fast configuration favors correctness over availability during bad rollouts; use canaries and retain rollback-compatible config.
    - **Follow-up:** Which settings can safely refresh without rebuilding clients or breaking in-flight work?
    - **Takeaway:** Validate the effective configuration as a system.

### Q4. Why can a busy scheduled relay stop replica-lag monitoring?

??? question "Reveal answer"
    - **Independent jobs can contend for one scheduler and make health samples stale.**
    - **Mechanism:** Boot's platform-thread scheduling defaults commonly use one scheduler thread. Fixed delay measures from completion; fixed rate targets a cadence but does not inherently overlap one periodic invocation on a normal scheduled executor.
    - **Failure:** a drain-until-empty relay monopolizes the scheduler. Every pod also schedules its own copy; increasing pool size does not solve distributed job ownership.
    - **Fix:** isolate long jobs, bound each run, persist claims, and expire health observations by sample age. Verify the actual scheduler, including virtual-thread configuration.
    - **Trade-off:** larger pools increase parallelism and downstream load; leader election simplifies ownership but limits throughput.
    - **Follow-up:** Should an old successful health sample still permit replica routing?
    - **Takeaway:** Scheduled health checks need a freshness contract.

### Q5. How do liveness, readiness, startup probes, and graceful shutdown work together?

??? question "Reveal answer"
    - **Separate restart decisions, traffic admission, startup tolerance, and draining.**
    - **Mechanism:** startup probes protect slow initialization; readiness controls endpoint eligibility; liveness requests restart. Spring shutdown gives supported in-flight work a bounded completion window.
    - **Failure:** dependency checks in liveness create restart storms. Shared-dependency readiness can remove every replica; background jobs and Kafka commits need their own shutdown analysis.
    - **Fix:** test dependency failure and SIGTERM under load. Budget termination for endpoint propagation, any preStop duration, and all shutdown phases; the Kubernetes grace countdown includes preStop.
    - **Trade-off:** remaining ready to degrade is appropriate only for useful available operations; do not report healthy success for incorrect business effects.
    - **Follow-up:** What happens if a payment call outlives the shutdown budget?
    - **Takeaway:** Lifecycle correctness must be tested, not inferred from YAML.

## Related

- [Concepts](concepts.md)
- [Interview Questions](questions.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
- [Deployment practice](../ci-cd/practice.md)
- [Boot 4 migration](../../tracks/java25-boot4/migration.md)
