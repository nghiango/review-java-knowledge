# Code Review: Silently Renamed Configuration Property

## Scenario

`LegacyMessagingSettings.java` and `OutboundNotifier.java` are part of a Spring Boot 3.5 → 4.0
upgrade. The outbound notification pipeline reads its endpoint, topic and batch size from
`application.yml` under the `notifications.outbound` prefix.

The pull request description says:

> *"The upgrade only touched the framework version. Configuration keys are unchanged, so nothing in
> the notification pipeline needs to move."*

A canary instance of this service starts cleanly, passes its health check, and serves traffic — but
the notification consumer reports that messages stop arriving.

## Review Objectives

1. If a bound property name no longer matches a key in `application.yml`, what happens at startup?
2. What does `publish` do when `topic` is null, and is that acceptable?
3. Would this configuration bean fail fast if it were misconfigured?
4. How would you make a misconfigured pipeline visible before it drops a single message?

Consider these dimensions:

- configuration binding and property precedence
- validation
- observability
- immutability of bound state
- failure handling

Write your findings down before opening `SOLUTION.md`.
