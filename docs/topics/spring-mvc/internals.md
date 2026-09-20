# Spring MVC Internals

A deep dive into the internal execution pipeline of Spring MVC: `DispatcherServlet.doDispatch()`, handler mapping resolution, argument resolver composites, return value processing, interceptor execution, and async dispatching.

## 1. The `DispatcherServlet.doDispatch()` Lifecycle

At the core of Spring MVC is `DispatcherServlet.doDispatch(HttpServletRequest request, HttpServletResponse response)`. The execution flow proceeds through well-defined phases:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant DS as DispatcherServlet
    participant HM as RequestMappingHandlerMapping
    participant HEC as HandlerExecutionChain
    participant HA as RequestMappingHandlerAdapter
    participant AR as HandlerMethodArgumentResolverComposite
    participant C as Controller Method
    participant HMC as HttpMessageConverter
    participant EH as HandlerExceptionResolverComposite

    Client->>DS: HTTP Request
    DS->>HM: getHandler(request)
    HM-->>DS: HandlerExecutionChain (Interceptors + HandlerMethod)
    
    DS->>HEC: applyPreHandle(request, response)
    alt Interceptor preHandle returns false
        HEC-->>DS: false
        DS->>Client: Request Halted
    end
    
    DS->>HA: handle(request, response, handler)
    HA->>AR: resolveArgument(parameter, mavContainer, webRequest, binderFactory)
    AR-->>HA: Bound and Validated Arguments
    HA->>C: invokeAndHandle(webRequest, mavContainer)
    
    alt Method throws Exception
        C-->>DS: Exception thrown
        DS->>EH: resolveException(request, response, handler, ex)
        EH-->>DS: ProblemDetail / ModelAndView
    else Normal Execution
        C-->>HA: Return Value (DTO)
        HA->>HMC: write(returnValue, selectedMediaType, outputMessage)
        HMC-->>HA: Serialized JSON Bytes
    end

    DS->>HEC: applyPostHandle(request, response, mv)
    DS->>HEC: triggerAfterCompletion(request, response, ex)
    DS-->>Client: HTTP Response
```

---

## 2. Handler Mapping & Lookup

When a request enters `doDispatch()`:

1. `DispatcherServlet` queries registered `HandlerMapping` beans in order of priority.
2. `RequestMappingHandlerMapping` scans all `@Controller` and `@RestController` beans during startup, populating a `MappingRegistry` with `RequestMappingInfo` keys.
3. It performs a path lookup and matches HTTP method, headers, params, and consumes/produces clauses.
4. It wraps the matched `HandlerMethod` with any applicable `HandlerInterceptor` instances into a `HandlerExecutionChain`.

---

## 3. Argument Resolution (`HandlerMethodArgumentResolverComposite`)

The `RequestMappingHandlerAdapter` delegates parameter resolution to a composite chain of `HandlerMethodArgumentResolver` implementations:

```mermaid
flowchart TD
    Param[Method Parameter] --> Comp[HandlerMethodArgumentResolverComposite]
    Comp -->|supportsParameter?| R1[RequestParamMethodArgumentResolver]
    Comp -->|supportsParameter?| R2[PathVariableMethodArgumentResolver]
    Comp -->|supportsParameter?| R3[RequestResponseBodyMethodProcessor]
    Comp -->|supportsParameter?| R4[ServletRequestMethodArgumentResolver]
    Comp -->|supportsParameter?| R5[Custom Argument Resolvers]

    R3 -->|resolveArgument| Jackson[MappingJackson2HttpMessageConverter]
    Jackson -->|Deserialize Body| DTO[Target DTO Instance]
    DTO -->|Validate if @Valid present| Validator[DataBinder / Jakarta Validator]
```

### Argument Resolver Execution Steps

1. **`supportsParameter(MethodParameter parameter)`**: Iterates through registered resolvers until one returns `true`.
2. **`resolveArgument(...)`**:
   - Reads request attributes, headers, or body stream.
   - Converts raw strings/streams into typed Java objects via `ConversionService` or `HttpMessageConverter`.
   - If `@Valid` or `@Validated` is present, triggers `WebDataBinder.validate()`.
   - If validation errors are detected and no `BindingResult` parameter immediately follows, throws `MethodArgumentNotValidException`.

---

## 4. Return Value Handling & `HttpMessageConverter` Pipeline

For `@RestController` methods (or `@ResponseBody` methods), `RequestResponseBodyMethodProcessor` acts as both an argument resolver and a return value handler:

1. **Content Negotiation**: Inspects the incoming `Accept` header and matching `@RequestMapping(produces = ...)` specifications.
2. **Converter Selection**: Iterates through registered `HttpMessageConverter` instances (e.g. `ByteArrayHttpMessageConverter`, `StringHttpMessageConverter`, `MappingJackson2HttpMessageConverter`).
3. **Serialization**: `MappingJackson2HttpMessageConverter` writes the serialized Java object directly to `response.getOutputStream()` using Jackson's `ObjectMapper`.
4. Marks the `ModelAndViewContainer` as resolved so no view resolution takes place.

---

## 5. Centralized Exception Handling (`HandlerExceptionResolver`)

When an unhandled exception escapes controller execution:

1. `DispatcherServlet.processHandlerException(...)` delegates to `HandlerExceptionResolverComposite`.
2. `ExceptionHandlerExceptionResolver` checks for `@ControllerAdvice` or `@RestControllerAdvice` beans containing matching `@ExceptionHandler` methods.
3. Method matching resolves by most specific exception class hierarchy.
4. For methods returning `ProblemDetail` or `@ResponseBody`, Jackson serializes the RFC 9457 error payload to the HTTP response with the configured HTTP status code.
5. Interceptors' `afterCompletion()` hooks are invoked with the resulting exception or completion status.

---

## 6. Async Request Processing Pipeline

When a controller returns a `DeferredResult<T>` or `CompletableFuture<T>`:

1. `AsyncWebRequest` starts Servlet 3.0 asynchronous processing via `request.startAsync()`.
2. The initial Tomcat worker thread completes `DispatcherServlet.doDispatch()` and is returned to the worker pool.
3. The underlying async task runs in a separate thread (e.g. `ForkJoinPool` or a custom `ThreadPoolTaskExecutor`).
4. When `deferredResult.setResult(value)` is called, the container performs an async dispatch back to `DispatcherServlet`.
5. `DispatcherServlet` handles the dispatch type `ASYNC`, resolving the result value with `HttpMessageConverter` and writing the HTTP response.
