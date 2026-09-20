# Maintainability and Design Issues

Coupling, weak abstractions, API contracts, encapsulation and architecture failures.

## Entries

### Exposed mutable collection

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

**Technology:** Core Java collections · **Interview frequency:** Medium · **Production impact:** Medium

Storing and returning a caller-owned list aliases mutable state across boundaries. Use `List.copyOf`
when taking a snapshot; an unmodifiable view alone still reflects mutation through the original list.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Extensible value-object equality

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** Core Java equality · **Interview frequency:** High · **Production impact:** High

Open inheritance and value equality can violate symmetry/transitivity when a subtype adds state.
Prefer a final immutable value type (a record where appropriate) or explicitly define identity
semantics for the hierarchy.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Inconsistent equals and hashCode state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

**Technology:** HashMap, HashSet · **Interview frequency:** High · **Production impact:** High

`equals` and `hashCode` must use compatible state: equal values must always have equal hashes.
Keep display/profile data outside the identity key and test the implication directly.

**Appears in:** [Core Java — mutable map key](../topics/core-java/code-review.md#mutable-map-key)

### Optional as mutable shared state

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

An Optional field on a singleton service is mutable cross-request state, not an absence contract.
Keep the service stateless or use an explicitly keyed, thread-safe cache.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Optional parameter obscures a required contract

**Type:** API design issue · **Severity:** Medium · **Difficulty:** Basic

An Optional parameter forces wrapping while still allowing a null Optional reference. Accept the
required value and validate it at the boundary; reserve Optional primarily for return absence.

**Appears in:** [Core Java — Optional and exceptions](../topics/core-java/code-review.md#optional-and-exception-misuse)

### Import operation couples I/O, parsing and mutation

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

When one method owns resource acquisition, syntax, validation and caller mutation, failure policy
cannot be tested or changed independently. Inject a source and return an explicit immutable result.

**Appears in:** [Core Java — resource and collection mutation](../topics/core-java/code-review.md#resource-and-collection-mutation)

### Duplicate listener registration

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Basic

A subscription API that does not model registration identity can retain and invoke the same listener
multiple times after retries or repeated startup hooks. Return a registration handle and define the
duplicate policy explicitly.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### Listener backing list escapes

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

Returning a registry's mutable listener list lets callers change global state outside the registry
contract. Return an immutable snapshot for inspection.

**Appears in:** `modules/02-jvm/broken-examples/static-listener-leak`

### Ambient request context lacks scope

**Type:** Design issue · **Severity:** High · **Difficulty:** Intermediate

An API that only sets ThreadLocal state hides ownership and cleanup. Return a closeable scope that
captures the previous value, restores parents and removes the value when no parent exists.

**Appears in:** `modules/02-jvm/broken-examples/threadlocal-pool-leak`

### Hidden static cache policy

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

Static cache policy cannot be configured, replaced or tested at the component boundary. Make the
capacity and eviction policy explicit, or inject a production cache implementation.

**Appears in:** `modules/02-jvm/broken-examples/unbounded-cache`

### Chained regex character replacement

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Basic

Chaining multiple `replaceAll` calls repeatedly parses regex patterns and allocates intermediate
string instances for each replacement. Prefer a single-pass character scan using a `StringBuilder`.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

### Untyped loose domain arguments

**Type:** Design issue · **Severity:** Medium · **Difficulty:** Intermediate

Passing loose primitives and mutable maps bypasses validation at domain construction time and allows
unvalidated or concurrently modified state. Model domain telemetry payloads as immutable records.

**Appears in:** `modules/02-jvm/broken-examples/excessive-hot-path-allocation`

### Field injection hides dependencies and prevents unit testing

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Basic

Injecting dependencies directly into private fields via `@Autowired` hides constructor contracts, prevents `final` immutable fields, and makes unit testing impossible without reflection or Spring runners. Use explicit constructor injection.

**Appears in:** [Spring Core — circular field injection](../topics/spring-core/code-review.md#circular-field-injection)

### Circular dependency between domain services

**Type:** Architecture issue · **Severity:** High · **Difficulty:** Intermediate

Direct bidirectional dependencies between beans (`ServiceA` $\leftrightarrow$ `ServiceB`) prevent isolated reasoning and trigger startup failures in modern Spring Boot. Decouple services using domain events (`ApplicationEventPublisher` / `@EventListener`).

**Appears in:** [Spring Core — circular field injection](../topics/spring-core/code-review.md#circular-field-injection)

### Self-invocation bypasses Spring AOP proxy interceptors

**Type:** Architecture issue · **Severity:** Critical · **Difficulty:** Intermediate

Calling an annotated method (`@Transactional`, `@Audited`, `@Async`, `@Cacheable`) on `this` within the same class stays inside the target instance and bypasses the Spring proxy interceptor chain, silently omitting cross-cutting behavior. Move the annotated method to a dedicated collaborator bean.

**Appears in:** [Spring Core — self-invocation aspect bypass](../topics/spring-core/code-review.md#self-invocation-aspect-bypass)

### Prototype bean injected into singleton is never re-created

**Type:** Scope issue · **Severity:** Critical · **Difficulty:** Intermediate

Injecting a `@Scope("prototype")` bean directly into a singleton constructor resolves and wires the prototype only once at container startup. Use `ObjectProvider<T>`, `@Lookup`, or `ScopedProxyMode.TARGET_CLASS` to dynamically fetch fresh instances.

**Appears in:** [Spring Core — prototype injection in singleton](../topics/spring-core/code-review.md#prototype-injection-in-singleton)

### Mutable instance state in Spring singleton bean

**Type:** Concurrency issue · **Severity:** Critical · **Difficulty:** Basic

Spring `@Service` and `@Component` beans are shared singletons. Storing per-request data in instance fields causes multithreaded race conditions and cross-tenant data leakage. Keep services strictly stateless and pass data via method parameters and records.

**Appears in:** [Spring Core — mutable singleton state](../topics/spring-core/code-review.md#mutable-singleton-state)

### Scattered `@Value` configuration without validation

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Intermediate

Direct `@Value` injection scatters property keys across services without type hierarchy, IDE autocomplete, or startup validation. Encapsulate properties in `@ConfigurationProperties` records annotated with `@Validated` and Jakarta Bean Validation constraints.

**Appears in:** `modules/05-spring-boot/broken-examples/scattered-value-config`

### Accidental auto-configuration override bypasses framework customization

**Type:** Architecture issue · **Severity:** High · **Difficulty:** Senior

Defining raw custom beans without using auto-configured builders or customizer callbacks accidentally overrides Spring Boot framework defaults, discarding Micrometer metrics, distributed tracing, and connection pool configuration. Use `@ConditionalOnMissingBean` and implement framework Customizer interfaces.

**Appears in:** `modules/05-spring-boot/broken-examples/accidental-autoconfig-override`

## Related

- [Issue catalogue](index.md)
