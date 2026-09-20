# Solution: Entity Leakage and Mass Assignment

## Annotated Code

### `UserAdminController.java`
```java
package lab.restapi.broken.dtosecurity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final Map<Long, UserAccount> userDatabase = new ConcurrentHashMap<>();

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getUser(@PathVariable Long id) {
        UserAccount user = userDatabase.get(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // Security issue: Exposing internal domain entity serializes sensitive fields (passwordHash, accountBalance) into public REST responses
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> updateUser(
            // Security issue: Binding domain entity directly from @RequestBody enables Mass Assignment (OWASP API6:2023), allowing clients to elevate privileges (role) and modify financial balances
            @PathVariable Long id, @RequestBody UserAccount payload) {
        UserAccount existingUser = userDatabase.get(id);
        if (existingUser == null) {
            return ResponseEntity.notFound().build();
        }

        existingUser.setUsername(payload.getUsername());
        existingUser.setEmail(payload.getEmail());
        existingUser.setRole(payload.getRole());
        existingUser.setAccountBalance(payload.getAccountBalance());

        return ResponseEntity.ok(existingUser);
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Mass Assignment / Privilege Escalation | Critical | Security | Binding request bodies directly to internal domain models allows malicious clients to overwrite protected fields (`role`, `accountBalance`). |
| Sensitive Information Disclosure | High | Security | Returning internal domain models leaks private authentication secrets (`passwordHash`) and internal ledger balances to API callers. |

---

## Remediation Strategy

1. **Use Explicit Request/Response DTO Records:**
   ```java
   public record UserResponse(Long id, String username, String email, String role) {}
   public record UpdateUserProfileRequest(@NotBlank String username, @Email String email) {}
   ```
2. **Explicit Mapping Layer:**
   Copy only allowable client-supplied fields from the validated DTO to the domain entity. Never expose setter methods for security-critical attributes on generic update endpoints.
