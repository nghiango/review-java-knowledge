# Spring Security Internals & Pipeline Architecture

## 1. The Delegation Chain: `DelegatingFilterProxy` to `FilterChainProxy`

Spring Security intercepts HTTP traffic by bridging the Servlet Container's native filter lifecycle to the Spring `ApplicationContext`.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant ServletContainer as Tomcat / Jetty Servlet Pipeline
    participant DFP as DelegatingFilterProxy
    participant AppContext as Spring WebApplicationContext
    participant FCP as FilterChainProxy
    participant SFC as SecurityFilterChain (Bean)
    participant AuthFilter as AuthorizationFilter

    Client->>ServletContainer: HTTP Request (GET /api/orders)
    ServletContainer->>DFP: doFilter(req, res, chain)
    Note over DFP: Looks up Spring bean named "springSecurityFilterChain"
    DFP->>AppContext: getBean("springSecurityFilterChain")
    AppContext-->>DFP: FilterChainProxy instance
    DFP->>FCP: doFilter(req, res, chain)
    FCP->>SFC: getFilters(request)
    Note over FCP,SFC: Iterates ordered filter chain
    SFC->>AuthFilter: doFilter(req, res, chain)
    AuthFilter-->>Client: Processed Response
```

---

## 2. Authentication Architecture: `AuthenticationManager` & `ProviderManager`

Authentication uses a strategy pattern where `AuthenticationManager` delegates to a list of registered `AuthenticationProvider` instances.

```mermaid
flowchart TD
    Req[Auth Request: UsernamePassword / Bearer Token] --> Filter[Authentication Filter]
    Filter --> Token[Create unauthenticated Authentication token]
    Token --> AuthManager[ProviderManager: implements AuthenticationManager]

    AuthManager --> P1{DaoAuthenticationProvider}
    AuthManager --> P2{JwtAuthenticationProvider}
    AuthManager --> P3{OAuth2ResourceServerAuthenticationProvider}

    P1 -->|supports? Yes| UDS[UserDetailsService#loadUserByUsername]
    UDS --> PE[PasswordEncoder#matches]
    PE -->|Match OK| AuthSuccess[Return authenticated UsernamePasswordAuthenticationToken]

    AuthSuccess --> ContextHolder[SecurityContextHolder#setContext]
```

---

## 3. `SecurityContextHolder` Architecture & Storage Strategies

Spring Security stores the current `Authentication` object inside a `SecurityContext`.

```mermaid
classDiagram
    class SecurityContextHolder {
        +getContext() SecurityContext
        +setContext(SecurityContext)
        +clearContext()
        +setStrategyName(String)
    }

    class SecurityContextHolderStrategy {
        <<interface>>
        +getContext() SecurityContext
        +setContext(SecurityContext)
        +clearContext()
    }

    class ThreadLocalSecurityContextHolderStrategy {
        -ThreadLocal~SecurityContext~ contextHolder
    }

    class InheritableThreadLocalSecurityContextHolderStrategy {
        -InheritableThreadLocal~SecurityContext~ contextHolder
    }

    SecurityContextHolder --> SecurityContextHolderStrategy
    SecurityContextHolderStrategy <|.. ThreadLocalSecurityContextHolderStrategy
    SecurityContextHolderStrategy <|.. InheritableThreadLocalSecurityContextHolderStrategy
```

### Storage Strategies
- **`MODE_THREADLOCAL` (Default)**: Bound to the executing OS thread. Safe for standard blocking Servlet containers.
- **`MODE_INHERITABLETHREADLOCAL`**: Spawns copy to child threads. Caution: dangerous on pooled executor threads where threads are reused.
- **`MODE_GLOBAL`**: Single JVM-wide static context (used in standalone desktop apps).

---

## 4. Method Security Pipeline: `AuthorizationManagerBeforeMethodInterceptor`

Spring Security 6+ uses AOP interceptors (`AuthorizationManagerBeforeMethodInterceptor`) to evaluate method-level annotations (`@PreAuthorize`, `@PostAuthorize`, `@Secured`, `@RolesAllowed`).

```mermaid
flowchart TD
    MethodCall[Service Method Invoked] --> Proxy[Spring AOP Proxy]
    Proxy --> Interceptor[AuthorizationManagerBeforeMethodInterceptor]
    Interceptor --> SpEL[Evaluate PreAuthorize Expression via MethodSecurityEvaluationContext]

    SpEL --> ExprCheck{Expression Evaluates to True?}
    ExprCheck -->|Yes| TargetMethod[Proceed to Target Service Method]
    ExprCheck -->|No| AccessDenied[Throw AccessDeniedException -> 403 Forbidden]
```
