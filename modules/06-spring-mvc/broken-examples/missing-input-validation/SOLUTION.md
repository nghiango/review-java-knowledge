# Solution: Missing Input Validation

## Annotated Code

### `CreateUserRequest.java`

```java
package lab.springmvc.broken.inputvalidation;

public class CreateUserRequest {

    // Validation issue: Missing @NotBlank and @Size constraints allows blank or excessively long usernames
    private String username;

    // Validation issue: Missing @NotBlank and @Email constraints allows malformed email addresses
    private String email;

    // Validation issue: Missing @PositiveOrZero or @DecimalMin constraint allows negative account balances
    private double initialBalance;

    public CreateUserRequest() {}

    public CreateUserRequest(String username, String email, double initialBalance) {
        this.username = username;
        this.email = email;
        this.initialBalance = initialBalance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }
}
```

### `UserController.java`

```java
package lab.springmvc.broken.inputvalidation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Validation issue: Omitting @Valid or @Validated allows invalid payloads through to the service layer without triggering MethodArgumentNotValidException
    @PostMapping
    public String createUser(@RequestBody CreateUserRequest request) {
        return userService.registerUser(request);
    }
}
```

## Issue List

| Location | Category | Description | Rationale |
|---|---|---|---|
| `CreateUserRequest.java:8` | `Validation` | Unvalidated username field | Permits `null`, empty string, and whitespace-only usernames. |
| `CreateUserRequest.java:11` | `Validation` | Unvalidated email field | Accepts strings that violate RFC email format specifications. |
| `CreateUserRequest.java:14` | `Validation` | Missing `@PositiveOrZero` constraint | Allows negative account balances into the financial domain. |
| `UserController.java:19` | `Validation` | Missing `@Valid` on `@RequestBody` parameter | Disables Spring MVC validation pipeline; controller proceeds directly with invalid data. |

## Correct implementation

- Package: `lab.springmvc.inputvalidation`
- Production reference: `CreateUserRequest.java` (immutable record with `@NotBlank`, `@Email`, `@PositiveOrZero`), `UserController.java` (`@Valid @RequestBody`), `UserService.java`
- Fix: Annotate request DTO records with Jakarta Bean Validation constraints and enforce `@Valid` on controller endpoint handler arguments. Handle `MethodArgumentNotValidException` globally in `@RestControllerAdvice`.
