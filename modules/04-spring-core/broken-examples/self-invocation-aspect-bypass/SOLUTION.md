# Solution: Spring AOP aspect bypassed by self-invocation

## Annotated code

```java
@Service
public class OrderProcessingService {

    public void processOrder(String orderId) {
        System.out.println("Processing order: " + orderId);

        // Architecture / Concurrency / Reliability issue: Self-invocation (calling auditOrder() on 'this')
        // completely bypasses the Spring proxy interceptor chain. The @Audited advice in AuditAspect is
        // never executed, leading to silent omission of compliance audit logs.
        auditOrder(orderId);
    }

    // Design issue: @Audited on a method intended to be called internally within the same class
    // fails silently under standard Spring AOP proxying.
    @Audited(action = "PROCESS_ORDER")
    public void auditOrder(String orderId) {
        System.out.println("Executing audit record for: " + orderId);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Architecture issue | Critical | `OrderProcessingService.processOrder()` | Self-invocation bypasses Spring AOP proxy interceptor |
| 2 | Reliability issue | Critical | `OrderProcessingService.auditOrder()` | Compliance audit logs are silently omitted |
| 3 | Design issue | Medium | Class responsibilities | Order processing and audit logging mixed in same bean |

## Issue details

### Spring AOP proxy mechanics and self-invocation

**Type:** Architecture issue · **Severity:** Critical · **Difficulty:** Intermediate

Spring AOP uses runtime proxies (CGLIB subclassing or JDK dynamic interface proxies). When an external caller invokes a method on a bean, the call enters the **proxy**, which executes the aspect advice (`AuditAspect.auditMethod`) before delegating to the actual target bean.

However, once execution enters `OrderProcessingService.processOrder()`, any subsequent call to `auditOrder()` or `this.auditOrder()` is executed directly on the **target instance (`this`)**, completely bypassing the proxy wrapper and its interceptor chain. As a result, `@Transactional`, `@Async`, `@Cacheable`, and custom `@Aspect` annotations on the called method are silently ignored.

### Recommended Fixes:
1. **Refactor into a dedicated collaborator bean (Preferred)**: Move the audited operation to `OrderAuditService` and inject it into `OrderProcessingService`. The method call will pass through the proxy of `OrderAuditService`.
2. **Self-injection**: Inject the bean itself (`@Autowired private OrderProcessingService self;` or `ObjectProvider<OrderProcessingService>`) and invoke `self.auditOrder(orderId)`.
3. **AspectJ compile-time or load-time weaving (LTW)**: Weaves bytecode directly without runtime proxies (applicable for complex domain models).

## Correct implementation

The production-ready fix lives in `lab.springcore.selfinvocation`:
- `OrderAuditService.java` dedicated bean encapsulating `@Audited` logging.
- `OrderProcessingService.java` cleanly delegating to `OrderAuditService` across proxy boundaries.
- `AuditAspect.java` intercepting calls reliably.
