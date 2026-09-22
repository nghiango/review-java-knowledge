# Solution: Silently Renamed Configuration Property

## Annotated code

```java
package lab.java25boot4.whatsnew.broken.propertybinding;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuration issue: the prefix and key names were inherited from Boot 3.5. Where a key was
// renamed or removed in Boot 4, binding simply finds nothing and leaves the field at its default
// (null / 0). Startup succeeds — there is no error to notice.
@ConfigurationProperties(prefix = "notifications.outbound")
public class LegacyMessagingSettings {

    private String endpoint;
    private String topic;
    private int batchSize;

    // Reliability issue: mutable JavaBean setters mean the bean exists in a half-bound state and
    // is shared before binding completes. Nothing rejects an instance that was never populated.
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
```

```java
package lab.java25boot4.whatsnew.broken.propertybinding;

public class OutboundNotifier {

    private final LegacyMessagingSettings settings;

    public OutboundNotifier(LegacyMessagingSettings settings) {
        this.settings = settings;
    }

    // Reliability issue: a missing topic is treated as "nothing to do" rather than a
    // configuration failure, so the pipeline drops every notification.
    // Observability issue: the drop is silent — no log, no metric, no health signal. The canary
    // looks healthy while messages disappear.
    public String publish(String payload) {
        if (settings.getTopic() == null) {
            return "skipped";
        }
        return "published "
                + payload
                + " to "
                + settings.getTopic()
                + " (batch="
                + settings.getBatchSize()
                + ")";
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Configuration issue | High | `LegacyMessagingSettings` | Boot 4 binding keys no longer match; fields stay null/0 with no error |
| 2 | Reliability issue | High | `LegacyMessagingSettings` | No `@Validated` / `@NotNull` — a misconfigured app starts successfully |
| 3 | Observability issue | High | `OutboundNotifier.publish()` | Missing configuration drops messages silently — no log, metric or health signal |
| 4 | Reliability issue | Medium | `LegacyMessagingSettings` | Mutable JavaBean binding exposes a half-bound object; a record is validated at construction |

## Issue details

## Renamed property binds to nothing without error

**Type:** Configuration issue · **Severity:** High · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `@ConfigurationProperties`, Spring Boot 4 binding
**Interview frequency:** High · **Production impact:** High

**Location:** `LegacyMessagingSettings` (prefix `notifications.outbound`)

### Problem
A property key that existed in Boot 3.5 and was renamed or removed in Boot 4 no longer matches the
bound field. Spring Boot binds what it finds and leaves the rest at its Java default, so `topic`
stays `null` and `batchSize` stays `0` while the application starts normally.

### Why it happens
Relaxed binding maps keys to fields; it does not require that every field has a key. Unknown keys are
ignored and missing keys are not errors unless validation is configured. An upgrade that renames a
key therefore produces no compile error, no startup error and no log line.

### Production impact
```text
application.yml : notifications.outbound.destination: orders   (renamed key)
bound field     : topic = null
→ publish() returns "skipped" for every message
→ consumer lag grows, canary health stays green
```

### Broken implementation
```java
@ConfigurationProperties(prefix = "notifications.outbound")
public class LegacyMessagingSettings {
    private String topic;   // stays null when the key no longer matches
}
```

### Correct implementation
```java
@ConfigurationProperties(prefix = "notifications.outbound")
@Validated
public record MessagingSettings(
        @NotBlank String endpoint, @NotBlank String topic, @Positive int batchSize) {}
```

### Why the solution works
A record with `@Validated` and Jakarta Bean Validation constraints makes every required key
mandatory. If a key no longer binds, the application fails at startup with a binding error naming the
property — the failure moves from production to the deploy step.

### Trade-offs
Every key becomes a hard requirement, so a service that legitimately runs without an optional
feature must model that with `Optional`/defaults and a conditional instead. Fail-fast also means a
missing config is now a deploy failure, which is the point.

### How to detect it
```bash
java -jar app.jar --debug 2>&1 | grep -i "notifications.outbound\|ConfigurationProperties"
```
`--debug` prints the binding report; keys that appear in `application.yml` but bind to nothing are
visible. In Boot 4, run with `--debug` and fix every "no longer valid" property report.

### Interview follow-up
> Why does Spring Boot not fail on an unknown or missing property by default, and what changes when
> you add `@Validated`?

### Related
- Configuration issue · `@ConfigurationProperties` · Relaxed binding · Fail-fast

## No validation on bound configuration

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Basic
**Track:** `java25-boot4` · **Technology:** `@Validated`, Jakarta Bean Validation
**Interview frequency:** High · **Production impact:** High

**Location:** `LegacyMessagingSettings`

### Problem
The bean has no `@Validated` annotation and no constraints, so there is nothing to reject an
unpopulated instance. A configuration object that can be empty is a configuration object that will
eventually be empty in production.

### Why it happens
Adding validation is easy to defer; the class "works" because the happy path binds every field.
Validation is exactly the feature that turns a silent misconfiguration into a loud failure.

### Production impact
A misconfigured instance passes the readiness probe and only fails when the first message is
processed — after deployment, during traffic.

### Broken implementation
```java
public class LegacyMessagingSettings {
    private String endpoint;   // no constraints
}
```

### Correct implementation
```java
@ConfigurationProperties(prefix = "notifications.outbound")
@Validated
public record MessagingSettings(
        @NotBlank String endpoint, @NotBlank String topic, @Positive int batchSize) {}
```

### Why the solution works
Jakarta Bean Validation constraints are evaluated during binding; a violation aborts application
startup with the offending property named in the exception.

### Trade-offs
Constraint messages become part of the startup contract and must be kept accurate. For genuinely
optional settings, use `Optional<T>` or a default rather than dropping validation.

### How to detect it
Grep configuration classes for `@ConfigurationProperties` without `@Validated`. Test by starting the
context with the property absent and asserting a `BindException`/validation failure.

### Interview follow-up
> How do you validate a configuration record in a test without starting the full application?

### Related
- Reliability issue · Jakarta Bean Validation · Fail-fast configuration

## Missing configuration drops messages silently

**Type:** Observability issue · **Severity:** High · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** Logging, metrics, health indicators
**Interview frequency:** High · **Production impact:** High

**Location:** `OutboundNotifier.publish()`

### Problem
When `topic` is null, `publish` returns the string `"skipped"` and does nothing else. There is no
log at WARN or ERROR, no counter for dropped notifications, and no health indicator that reports the
pipeline as unconfigured.

### Why it happens
Returning a sentinel string is the easiest way to keep the method total. It converts an operational
failure into a normal return value that no monitoring observes.

### Production impact
Messages disappear with a healthy-looking service. The first signal is a downstream consumer's
missing data — often hours later, from another team.

### Broken implementation
```java
if (settings.getTopic() == null) {
    return "skipped";
}
```

### Correct implementation
```java
public void publish(String payload) {
    // Fail fast: validation already guarantees these are non-blank.
    publisher.send(settings.topic(), payload);
}
```
plus a health indicator or startup log that states the resolved topic and endpoint.

### Why the solution works
Combining fail-fast validation with an explicit startup log means a misconfigured pipeline is
visible at deploy time, and a runtime failure throws instead of silently succeeding.

### Trade-offs
Failing fast removes the ability to "degrade gracefully" by skipping notifications. If skipping is
genuinely required, it must be an explicit, counted, alerted state — not a default.

### How to detect it
```bash
curl -s localhost:8080/actuator/health | jq
```
Add a custom health indicator that reports the outbound pipeline as `DOWN` when unconfigured, and a
`notifications.dropped` counter with an alert.

### Interview follow-up
> How do you distinguish "feature disabled on purpose" from "feature misconfigured" in monitoring?

### Related
- Observability issue · Health indicators · Fail-fast

## Mutable binding exposes a half-bound object

**Type:** Reliability issue · **Severity:** Medium · **Difficulty:** Intermediate
**Track:** `java25-boot4` · **Technology:** `@ConfigurationProperties`, records
**Interview frequency:** Medium · **Production impact:** Medium

**Location:** `LegacyMessagingSettings`

### Problem
Binding uses public setters, so the object is constructed empty and mutated field by field. Any code
that observes the bean during binding — or a bean that captures it before binding completes — sees a
partially populated configuration.

### Why it happens
Mutable JavaBean binding is the historical default. Records with constructor binding are the modern
form: the bound state is assembled and validated before the object exists.

### Production impact
A half-bound configuration can be read by an early initialiser, producing behaviour that depends on
bean-creation order — a non-deterministic, environment-specific bug.

### Broken implementation
```java
public class LegacyMessagingSettings {
    private String endpoint;
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
}
```

### Correct implementation
```java
public record MessagingSettings(
        @NotBlank String endpoint, @NotBlank String topic, @Positive int batchSize) {}
```

### Why the solution works
A record is immutable and bound through its canonical constructor, so it either exists fully valid or
does not exist at all. It also makes the required keys explicit in the type.

### Trade-offs
Records cannot express defaults as easily as mutable fields; use a compact constructor or an
`Optional` component for optional keys. The immutability is worth it for configuration.

### How to detect it
Prefer records for `@ConfigurationProperties`; flag JavaBean-style configuration classes in review.

### Interview follow-up
> Why are records the preferred shape for `@ConfigurationProperties` in modern Spring Boot?

### Related
- Reliability issue · Constructor binding · Records

## Correct implementation

Package `lab.java25boot4.whatsnew.propertybinding`, sources
`src/main/java/lab/java25boot4/whatsnew/propertybinding/MessagingSettings.java` and
`OutboundPublisher.java`, tests in `MessagingSettingsTest`.

Walkthrough and trade-offs: [What's New — Solutions](../../../../../docs/tracks/java25-boot4/whats-new/solutions.md#silently-renamed-configuration-property).
