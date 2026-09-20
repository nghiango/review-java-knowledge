# Solution: Entity Exposed Through API

## Annotated Code

### `UserProfileController.java`
```java
package lab.jpahibernate.broken.dtomapping;

import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final EntityManager entityManager;

    public UserProfileController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<UserProfile> getUser(@PathVariable Long id) {
        UserProfile user = entityManager.find(UserProfile.class, id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        // Security issue: Returning JPA entity directly serializes sensitive internal fields (passwordHash, internal state) into public API responses
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<UserProfile> updateUser(
            // Security issue: Direct entity binding in request body enables Mass Assignment / Privilege Escalation (e.g. client updating admin flag or passwordHash)
            @PathVariable Long id, @RequestBody UserProfile request) {
        UserProfile user = entityManager.find(UserProfile.class, id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setAdmin(request.isAdmin());

        return ResponseEntity.ok(user);
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Sensitive Field Exposure | High | Security | Returning the `UserProfile` JPA entity directly leaks `passwordHash` and internal entity metadata to API consumers. |
| Mass Assignment Vulnerability | High | Security | Accepting the entity directly in `@RequestBody` allows untrusted clients to supply values for privileged fields (such as `admin` flag or `id`), leading to unauthorized privilege escalation. |
| Coupling and Serialization Failures | Medium | Maintainability | Exposing entities directly tightly couples the database schema to the public REST contract, breaking API versioning and causing serialization crashes if lazy relationships are present. |

---

## Remediation Strategy

1. **Use Dedicated Request/Response DTOs (Java Records):**
   ```java
   public record UserProfileResponse(Long id, String username, String email, boolean admin) {}
   public record UpdateUserProfileRequest(String username, String email) {}
   ```
2. **Explicit Mapping Layer:**
   Map only validated, authorized fields between DTOs and the persistent entity. Never update security-critical properties (`admin`, `roles`, `passwordHash`) via generic profile update endpoints.
3. **DTO Projection Queries:**
   Query directly for DTO records when reading data for presentation to minimize database payload and eliminate entity detachment issues.
