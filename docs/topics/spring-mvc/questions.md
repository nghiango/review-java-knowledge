# Spring MVC Interview Questions

Four levels of interview questions covering DispatcherServlet request lifecycles, HTTP routing, validation, exception handling, async processing, CORS security, and production incident remediation.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is the role of `DispatcherServlet` in Spring MVC?

??? question "Reveal answer"
    `DispatcherServlet` is the front controller in Spring MVC. It receives all incoming HTTP requests and coordinates request routing via `HandlerMapping`, handler invocation via `HandlerAdapter`, argument resolution, view resolution or message conversion, and centralized exception handling.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q01DispatcherServletFlowExample.java"
        ```

### 2. What is the difference between `HandlerMapping` and `HandlerAdapter`?

??? question "Reveal answer"
    - **`HandlerMapping`**: Determines **which** handler (controller method) should process the incoming request based on URL, HTTP method, headers, or parameters.
    - **`HandlerAdapter`**: Knows **how** to invoke that handler. It encapsulates argument resolution, parameter type conversion, validation triggering, method execution, and return value handling.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q02HandlerMappingVsHandlerAdapterExample.java"
        ```

### 3. How do argument resolvers work in Spring MVC?

??? question "Reveal answer"
    Spring MVC uses `HandlerMethodArgumentResolver` strategies to populate controller method parameters from HTTP request data (e.g. `@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestHeader`, `@AuthenticationPrincipal`).
    
    The `HandlerMethodArgumentResolverComposite` iterates registered resolvers and calls `resolveArgument()` on the first matching resolver.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q03ArgumentResolversExample.java"
        ```

### 4. What is the role of `HttpMessageConverter` and how does Jackson integrate with Spring MVC?

??? question "Reveal answer"
    `HttpMessageConverter` reads Java objects from HTTP request bodies (`read()`) and writes Java objects to HTTP response bodies (`write()`) based on media types.
    
    `MappingJackson2HttpMessageConverter` uses Jackson's `ObjectMapper` to automatically deserialize incoming JSON payloads into Java records/DTOs and serialize controller return values into JSON responses.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q04HttpMessageConverterJacksonExample.java"
        ```

### 5. How does declarative input validation work in Spring MVC with Jakarta Validation?

??? question "Reveal answer"
    Placing `@Valid` or `@Validated` on a `@RequestBody` controller parameter triggers Jakarta Bean Validation against the DTO's annotations (`@NotBlank`, `@Email`, `@Size`, `@Min`).
    
    If violations occur and no `BindingResult` parameter immediately follows, Spring throws `MethodArgumentNotValidException` (mapped to HTTP 400 Bad Request).

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q05InputValidationBindingResultExample.java"
        ```

### 6. How does `@RestControllerAdvice` work with RFC 9457 `ProblemDetail`?

??? question "Reveal answer"
    `@RestControllerAdvice` provides centralized, cross-cutting exception interception. In Spring Boot 3+, handler methods can return `ProblemDetail` instances standardizing error structures according to RFC 9457 with status codes, titles, error details, and custom diagnostic properties.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q06RestControllerAdviceProblemDetailExample.java"
        ```

### 7. What is the execution lifecycle order of Servlet Filters versus `HandlerInterceptor`s?

??? question "Reveal answer"
    1. Servlet `Filter` (`doFilter` pre-chain)
    2. `DispatcherServlet`
    3. `HandlerInterceptor.preHandle()`
    4. Controller handler method execution
    5. `HandlerInterceptor.postHandle()`
    6. `HandlerInterceptor.afterCompletion()`
    7. Servlet `Filter` (`doFilter` post-chain)

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q07FilterVsInterceptorLifecycleExample.java"
        ```

### 8. How should CORS be configured securely in Spring MVC?

??? question "Reveal answer"
    Configure explicit trusted origins via `allowedOrigins()` or trusted patterns via `allowedOriginPatterns("https://*.example.com")`.
    
    Never configure wildcard origins (`*`) while enabling credentials (`allowCredentials(true)`), as this creates critical cross-origin security vulnerabilities.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q08CorsConfigurationSecurityExample.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 9. How does asynchronous request processing work with `DeferredResult` and `Callable`?

??? question "Reveal answer"
    When a controller returns `DeferredResult` or `Callable`, `DispatcherServlet` initiates Servlet 3.0 asynchronous processing (`startAsync()`), immediately releasing the container's Tomcat worker thread back to the pool.
    
    A background thread computes the result and completes the `DeferredResult`, which triggers an async dispatch back to `DispatcherServlet` to write the response.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q09AsyncDeferredResultCallableExample.java"
        ```

### 10. What is `RestClient` in Spring Framework 6.1+ and how does it compare to `RestTemplate`?

??? question "Reveal answer"
    `RestClient` is the modern, synchronous fluent HTTP client introduced in Spring 6.1 (Spring Boot 3.2). It provides the modern fluent API style of `WebClient` while backed by synchronous HTTP libraries (e.g. `JdkClientHttpRequestFactory`, Apache HttpComponents), serving as the recommended replacement for `RestTemplate`.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q10RestClientFluentApiExample.java"
        ```

### 11. How does Content Negotiation work in Spring MVC?

??? question "Reveal answer"
    Content negotiation resolves the response representation media type based on the client's HTTP `Accept` header and the controller's `@GetMapping(produces = ...)` declarations.
    
    Spring matches compatible `HttpMessageConverter` instances to serialize the response into the requested format (e.g. JSON vs XML).

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q11ContentNegotiationExample.java"
        ```

### 12. How should multipart file uploads be handled and secured against malicious files?

??? question "Reveal answer"
    Handle file uploads using `MultipartFile` parameters with strict validation:
    - Enforce maximum upload size limits in configuration.
    - Validate file extensions against a strict whitelist.
    - Inspect MIME content types and magic bytes.
    - Sanitize original filenames to prevent path traversal attacks (`../`).

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q12MultipartFileUploadSecurityExample.java"
        ```

### 13. How do Jakarta Validation Groups work for differentiating Create and Update payloads?

??? question "Reveal answer"
    Validation groups allow applying different validation rules to the same model class depending on the operation.
    
    For example, resource creation requires the ID to be `@Null(groups = OnCreate.class)`, while resource update requires `@NotNull(groups = OnUpdate.class)`. The controller activates specific groups using `@Validated(OnCreate.class)`.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q13ValidationGroupsExample.java"
        ```

### 14. How do you implement a custom `HandlerMethodArgumentResolver`?

??? question "Reveal answer"
    Implement `HandlerMethodArgumentResolver` by overriding:
    1. `supportsParameter()`: Checks if the resolver applies to the target method parameter type or annotation.
    2. `resolveArgument()`: Extracts headers, session attributes, or security contexts and constructs the domain object.
    
    Register the resolver in `WebMvcConfigurer.addArgumentResolvers()`.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q14CustomArgumentResolverExample.java"
        ```

### 15. How does Server-Sent Events (SSE) streaming work with `SseEmitter`?

??? question "Reveal answer"
    `SseEmitter` keeps an HTTP connection open using `Content-Type: text/event-stream`. The server pushes incremental asynchronous events (`emitter.send()`) to the client over time and calls `emitter.complete()` when finished, without holding a Tomcat worker thread for the entire connection duration.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q15SseEmitterStreamingExample.java"
        ```

### 16. How do HTTP caching headers (`Cache-Control`, `ETag`) work in Spring MVC?

??? question "Reveal answer"
    Spring MVC supports HTTP caching via `CacheControl` builders on `ResponseEntity` and `ETag` headers.
    
    Clients send conditional `If-None-Match` headers; if the resource has not changed, Spring returns `304 Not Modified` with an empty body, saving bandwidth and server processing.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q16HttpCachingHeadersExample.java"
        ```

### 24. How does asynchronous HTTP streaming work with StreamingResponseBody vs ResponseBodyEmitter?

??? question "Reveal answer"

    **Short Answer:** `StreamingResponseBody` streams raw binary/text bytes directly to the client's `OutputStream` on an application worker thread without buffering the full payload in heap memory; `ResponseBodyEmitter` streams serialized objects converted via `HttpMessageConverter` over time.

    **Internal Mechanism:** When a controller returns `StreamingResponseBody`, Spring MVC initiates Servlet 3.0+ asynchronous processing, frees the container thread, and schedules a worker thread to write chunks directly to `response.getOutputStream()`, flushing per chunk.

    **Common Mistake:** Buffering large reports, CSVs, or files into a `byte[]` or `ByteArrayOutputStream` before returning a `ResponseEntity`, risking JVM heap `OutOfMemoryError` on high-concurrency downloads. [Concepts](/topics/spring-mvc/concepts.md#5-asynchronous-request-processing)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q24StreamingResponseBodyExample.java"
        ```

### 25. How do you validate and secure multipart file uploads against path traversal and file exhaustion?

??? question "Reveal answer"

    **Short Answer:** Enforce strict file size limits (`spring.servlet.multipart.max-file-size`), validate MIME types via content inspection rather than raw client extension headers, and sanitize filenames by stripping directory traversal sequences (`..`, `/`, `\`).

    **Internal Mechanism:** Attackers submit filenames like `../../etc/cron.d/malicious.jpg` (Zip Slip / Path Traversal) or multi-gigabyte multipart files (Denial of Service). Spring MVC parses multipart uploads via `StandardServletMultipartResolver`, requiring application validation on `MultipartFile` properties.

    **Common Mistake:** Using `file.getOriginalFilename()` directly in file system paths without sanitization, allowing arbitrary file overwrites outside the target upload directory. [Concepts](/topics/spring-mvc/concepts.md#3-input-validation-jakarta-validation)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q25MultipartUploadSecurityExample.java"
        ```

<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 17. How do you implement custom Jackson serializers for sensitive data masking?

??? question "Reveal answer"
    Extend `JsonSerializer<T>` and override `serialize(value, gen, serializers)` to apply masking rules (e.g. masking credit card numbers except the last 4 digits).
    
    Register the serializer via `@JsonSerialize(using = ...)` or globally through a Jackson `SimpleModule` bean.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q17JacksonCustomSerializerExample.java"
        ```

### 18. How does Servlet 3.0+ asynchronous request processing decouple container worker threads?

??? question "Reveal answer"
    In synchronous processing, a Tomcat worker thread is occupied for the entire duration of the request.
    
    In Servlet 3.0+ async processing, the container thread enters `DispatcherServlet`, calls `startAsync()`, and returns immediately to the pool. When the worker thread finishes, it dispatches an `ASYNC` event, allowing another container thread to serialize and write the response.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q18ServletRequestThreadModelExample.java"
        ```

### 19. How should resource creation URIs be generated using `UriComponentsBuilder`?

??? question "Reveal answer"
    `UriComponentsBuilder` builds RFC-compliant URLs dynamically. It expands path template variables (`buildAndExpand(id)`), applies URL encoding (`encode()`), and returns `URI` objects suitable for HTTP `201 Created` responses with `Location` headers.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q19UriComponentsBuilderLocationExample.java"
        ```

### 20. How do you attach custom extension properties to RFC 9457 `ProblemDetail` objects?

??? question "Reveal answer"
    `ProblemDetail.setProperty(key, value)` allows attaching custom structured diagnostic properties (such as validation error lists, trace IDs, timestamp metadata, or violation codes) without breaking RFC 9457 schema compliance.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q20ProblemDetailRfc9457ExtensionsExample.java"
        ```

### 21. How do you configure HTTP interceptors on `RestClient` using `ClientHttpRequestInterceptor`?

??? question "Reveal answer"
    `ClientHttpRequestInterceptor` intercepts outgoing HTTP requests made by `RestClient`.
    
    It allows injecting authentication tokens (e.g. OAuth2 Bearer tokens), distributed tracing headers (`X-Correlation-Id`), logging request/response payloads, and measuring downstream latency.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q21RestClientCustomizerInterceptorsExample.java"
        ```

### 26. Why is ForwardedHeaderFilter essential when generating absolute URIs with UriComponentsBuilder behind reverse proxies?

??? question "Reveal answer"

    **Short Answer:** Reverse proxies and API gateways terminate TLS and forward requests to internal instances over plain HTTP. `ForwardedHeaderFilter` adapts request host, scheme, and port from trusted `X-Forwarded-*` headers, preventing host header injection and broken `201 Created` Location URLs.

    **Deep Explanation:** When a client sends a request to `https://api.example.com`, the reverse proxy forwards it to `http://internal-pod:8080`. Without `ForwardedHeaderFilter`, `ServletUriComponentsBuilder.fromCurrentRequest()` generates internal URLs like `http://internal-pod:8080/api/orders/42` in redirect or `Location` response headers, leaking internal topology and breaking HTTPS navigation.

    **Internal Mechanism:** `ForwardedHeaderFilter` wraps the `HttpServletRequest` to override `getScheme()`, `getServerName()`, and `getServerPort()` from `Forwarded` or `X-Forwarded-Proto`/`X-Forwarded-Host` headers while discarding untrusted upstream proxy values.

    **Example:** [URI building and security](/topics/spring-mvc/concepts.md#1-front-controller-pattern-dispatcherservlet).

    **Common Mistake:** Enabling forwarded header reflection without restricting trusted proxy IP ranges (`server.forward-headers-strategy=framework`), allowing attackers to spoof `X-Forwarded-Host` for password reset link poisoning.

    **Production Consideration:** Configure `server.forward-headers-strategy=framework` and validate that ingress load balancers sanitize untrusted client headers before forwarding.

    **Follow-up Questions:**
    - How do REST APIs construct hypermedia links and resource URIs securely? See [REST API: Hypermedia and URIs](/topics/rest-api/questions.md)
    - What security headers prevent man-in-the-middle downgrade attacks? See [Spring Security: Security Headers](/topics/spring-security/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q26ForwardedHeaderSecurityExample.java"
        ```

### 27. How do you implement a custom HttpMessageConverter for proprietary binary protocols in Spring MVC?

??? question "Reveal answer"

    **Short Answer:** Extend `AbstractHttpMessageConverter<T>`, declare supported `MediaType`s, and implement `readInternal` / `writeInternal` to serialize domain objects to binary wire formats (e.g. Protocol Buffers, FlatBuffers).

    **Deep Explanation:** High-throughput microservice communication often trades human-readable JSON for compact binary serialization. Spring MVC evaluates the request's `Content-Type` and `Accept` headers against registered `HttpMessageConverter` beans during `HandlerAdapter` invocation.

    **Internal Mechanism:** `RequestResponseBodyMethodProcessor` iterates through converter candidates until `converter.canRead()` or `converter.canWrite()` matches the negotiated `MediaType`.

    **Example:** [Message converters](/topics/spring-mvc/concepts.md#1-front-controller-pattern-dispatcherservlet).

    **Common Mistake:** Adding custom converters to `configureMessageConverters` instead of `extendMessageConverters`, which inadvertently overwrites default Jackson and string converters.

    **Production Consideration:** Support both JSON and binary media types using HTTP Content Negotiation to allow public clients to consume JSON while internal services utilize compact binary payloads.

    **Follow-up Questions:**
    - How does Spring MVC resolve media types using HTTP Content Negotiation? See [REST API: Content Negotiation](/topics/rest-api/questions.md#19-how-does-spring-mvc-internally-execute-http-content-negotiation)
    - How do binary serialization protocols compare with JSON in CPU and memory profiling? See [Performance: Serialization Benchmarks](/topics/performance/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q27ProtobufHttpMessageConverterExample.java"
        ```

### 28. How does Spring MVC handle asynchronous request timeouts with DeferredResult and custom error resolution?

??? question "Reveal answer"

    **Short Answer:** `DeferredResult.onTimeout()` defines callback logic when background worker processing exceeds a configured timeout deadline, allowing the controller to return a clean HTTP `504 Gateway Timeout` or `408 Request Timeout` instead of hanging client connections.

    **Deep Explanation:** In asynchronous request processing, if a background worker hangs indefinitely (e.g. deadlocked or slow external dependency), the client connection remains open. Configuring a timeout on `DeferredResult` registers an OS timer. If the timer elapses before `setResult()` is called, Spring MVC triggers `onTimeout()` and dispatches an error response.

    **Internal Mechanism:** `StandardServletAsyncWebRequest` tracks timeout handlers using the container's `AsyncContext.addListener()`.

    **Example:** [Async processing](/topics/spring-mvc/concepts.md#5-asynchronous-request-processing).

    **Common Mistake:** Forgetting to set a result or error result inside `onTimeout()`, causing the servlet container to throw an unhandled `AsyncRequestTimeoutException` that renders an unformatted 500 error.

    **Production Consideration:** Always pair `onTimeout()` with background task cancellation (`future.cancel(true)`) to prevent zombie worker threads from consuming resources after the client has been sent a timeout response.

    **Follow-up Questions:**
    - How do CompletableFuture asynchronous pipelines handle stage timeouts with `orTimeout()`? See [Concurrency: CompletableFuture Composition](/topics/concurrency/questions.md#16-how-do-thenapply-thencompose-and-thencombine-differ-in-completablefuture)
    - How do Resilience4j TimeLimiter and CircuitBreaker interact with asynchronous controllers? See [Resilience: Fault Tolerance Patterns](/topics/resilience/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q28AsyncTimeoutHandlingExample.java"
        ```

<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Production Scenarios

### 22. Production Incident: Tomcat HTTP worker thread pool exhaustion under downstream service latency. How do you diagnose and remediate?

??? question "Reveal answer"
    **Symptom**: Application stops responding to incoming requests; health check probes fail; thread dumps show all 200 Tomcat worker threads in `WAITING` or `TIMED_WAITING` state.
    
    **Root Cause**: Synchronous HTTP controller endpoints invoking slow downstream microservices or heavy report generation without timeouts, holding Tomcat worker threads hostage.
    
    **Remediation**:
    1. Configure strict connect/read timeouts on HTTP clients.
    2. Refactor long-running endpoints to use `DeferredResult` or `CompletableFuture` running on dedicated, bounded thread pools (`ThreadPoolTaskExecutor`).
    3. Apply Resilience4j circuit breakers and rate limiters.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q22TomcatThreadStarvationScenarioExample.java"
        ```

### 23. Security Incident: CORS wildcard origin misconfiguration allowing cross-origin credential theft. How do you diagnose and remediate?

??? question "Reveal answer"
    **Symptom**: Penetration testing or security audit discovers that sensitive authenticated endpoints allow requests from arbitrary origins with credentials enabled.
    
    **Root Cause**: Misconfigured CORS policies using `allowedOriginPatterns("*")` or dynamic reflection of the `Origin` header paired with `allowCredentials(true)`.
    
    **Remediation**:
    1. Replace wildcard origins with an explicit whitelist of trusted frontend domains (`allowedOrigins("https://app.company.com")`).
    2. Restrict allowed HTTP methods to only required verbs (`GET`, `POST`, `PUT`, `DELETE`).
    3. Configure `maxAge` to cache preflight responses and reduce preflight traffic.

    ??? example "Example"
        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q23CorsOriginCredentialVulnerabilityScenarioExample.java"
        ```

### 29. Production Incident: Tomcat HTTP thread pool starvation caused by un-timed synchronous downstream calls

??? question "Reveal answer"

    **Short Answer:** A slow external payment or identity service caused request latency to spike from 50ms to 8s; within seconds, all 200 Tomcat worker threads became blocked waiting on socket reads, rejecting new incoming connections with 503/504 errors.

    **Deep Explanation:** In standard synchronous servlet architectures, one Tomcat thread is pinned per HTTP connection. When an un-timed downstream call blocks, the thread cannot serve other users. When all `server.tomcat.threads.max` threads are parked on socket I/O, the TCP accept queue fills and the server drops traffic.

    **Internal Mechanism:** Embedded Tomcat's `NioEndpoint` worker executor exhausts `maxThreads`, transitioning from active worker execution to thread pool starvation.

    **Example:** [Tomcat worker exhaustion](/topics/spring-mvc/code-review.md).

    **Common Mistake:** Sizing Tomcat `maxThreads` to 1000+, which increases context-switching overhead, kernel thread memory consumption, and exhausts database connection pools without fixing the root cause.

    **Production Consideration:** Enforce strict connect (2s) and read (3s) timeouts on all `RestClient` instances; use Circuit Breakers (`Resilience4j`) with fallbacks; enable virtual threads in Spring Boot 3.2+ for I/O-bound endpoints.

    **Follow-up Questions:**
    - How do thread pool sizing parameters and queue limits behave under high concurrency? See [Concurrency: ThreadPoolExecutor Configuration](/topics/concurrency/questions.md#14-what-are-the-core-parameters-of-threadpoolexecutor-and-how-do-its-4-rejection-policies-work)
    - How does the Bulkhead pattern isolate cascading thread pool exhaustion across microservices? See [Resilience: Bulkhead Pattern](/topics/resilience/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q29TomcatWorkerExhaustionScenarioExample.java"
        ```

### 30. Production Incident: Jackson infinite recursion stack overflow error on bidirectional entity serialization

??? question "Reveal answer"

    **Short Answer:** Returning JPA entities directly from `@RestController` methods with bidirectional `@OneToMany` / `@ManyToOne` associations causes Jackson to serialize parent $\to$ child $\to$ parent $\to$ child infinitely until `StackOverflowError` or HTTP 500 error cascade.

    **Deep Explanation:** Jackson's `ObjectMapper` inspects entity getter methods reflectively. When a parent `Department` has `getEmployees()` and child `Employee` has `getDepartment()`, Jackson traverses both getters recursively. This not only consumes CPU and crashes with `StackOverflowError`, but also triggers lazy loading of entire database graphs.

    **Internal Mechanism:** Jackson's `BeanSerializer` invokes object property writers recursively without cycle detection unless explicitly configured.

    **Example:** [Jackson circular references](/topics/spring-mvc/code-review.md).

    **Common Mistake:** Adding `@JsonIgnore` haphazardly to entity fields, which couples database persistence entities to presentation serialization concerns.

    **Production Consideration:** Never return JPA entities directly from REST controllers. Map entity state to immutable Java records (DTOs) with explicit response shapes, completely decoupling internal ORM relationships from external API contracts.

    **Follow-up Questions:**
    - How does the `@JsonManagedReference` and `@JsonBackReference` annotation pair break circular serialization? See [JPA / Hibernate: Entity Associations](/topics/jpa-hibernate/questions.md#11-what-is-the-difference-between-joincolumn-and-mappedby)
    - How do DTO projections prevent Mass Assignment vulnerabilities in Spring Boot? See [REST API: Mass Assignment Prevention](/topics/rest-api/questions.md#16-what-is-mass-assignment-vulnerability-cwe-915-and-how-do-dtos-prevent-it)

    ??? example "Example"

        ```java
        --8<-- "modules/06-spring-mvc/src/examples/java/lab/springmvc/questions/Q30JsonCircularReferenceScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->
