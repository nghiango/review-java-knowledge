# Solution: JSpecify Nullness Contract Violation

## Issues Identified

```java
@org.jspecify.annotations.NullMarked
package lab.java25boot4.springmvc.broken.nullness;
```

```java
package lab.java25boot4.springmvc.broken.nullness;

public record CustomerProfileResponse(
        String customerId,
        String fullName,
        // Contract issue: In a @NullMarked package, unannotated types are non-null by default. Storing null here violates the API contract and triggers compiler/linter warnings or NullPointerExceptions in callers
        String phoneNumber
) {}
```

```java
package lab.java25boot4.springmvc.broken.nullness;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerProfileController {

    @GetMapping("/{id}")
    public CustomerProfileResponse getCustomer(
            @PathVariable String id,
            // API design issue: @RequestParam(required = false) can inject null, violating the @NullMarked parameter contract unless explicitly annotated with @Nullable
            @RequestParam(required = false) String defaultPhone) {
        return new CustomerProfileResponse(id, "Customer " + id, defaultPhone);
    }
}
```

### 1. Contract issue (Implicit Non-Null Violation in `@NullMarked` Scope)
Under JSpecify's `@NullMarked`, every reference type is strictly non-null unless annotated with `@Nullable`. Declaring `String phoneNumber` without `@Nullable` promises clients and static analysis that the field is never null. Passing a null value breaches this contract.

### 2. API design issue (Unmarked Optional Controller Parameters)
When `@RequestParam(required = false)` is used, Spring passes `null` if the parameter is absent in the HTTP request. If the parameter type is not explicitly marked with `@Nullable`, the method signature falsely promises non-null input.

## Refactored Solution (Explicit JSpecify Null Annotations)
Annotate optional parameters and response fields with `@Nullable`:

```java
public record CustomerProfileResponse(
        String customerId,
        String fullName,
        @Nullable String phoneNumber
) {}
```

```java
public CustomerProfileResponse getCustomer(
        @PathVariable String id,
        @RequestParam(required = false) @Nullable String defaultPhone) { ... }
```
