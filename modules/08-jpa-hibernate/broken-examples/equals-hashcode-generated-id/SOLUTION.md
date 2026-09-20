# Solution: Equals and HashCode on Generated ID

## Annotated Code

```java
package lab.jpahibernate.broken.generatedid;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String fullName;

    public UserAccount() {}

    public UserAccount(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    // Reliability issue: equals relies on generated ID which is null for transient entities and mutates upon persist
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccount other)) return false;
        return id != null && id.equals(other.id);
    }

    // Reliability issue: hashCode changes after entity is persisted, breaking HashSet/HashMap bucket lookups
    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
```

## Issues Identified

### Reliability issue: equals and hashCode on generated ID break Set contracts when persisted
- **Location:** `UserAccount.java#equals`, `UserAccount.java#hashCode`
- **Explanation:** When a new `UserAccount` entity is created, its `id` is `null`. If the entity is inserted into a `HashSet`, its bucket index is computed based on `Objects.hashCode(null)` ($0$). When the entity is persisted and the database assigns an identity ID (e.g. $101$), the `hashCode()` changes to $101$. Future calls to `set.contains(account)` or `set.remove(account)` search bucket $101$ instead of bucket $0$, making the entity unfindable and causing silent duplicate inserts. Entities should base equality on an immutable business key (e.g. `email`) or a pre-assigned UUID.

## Correct implementation

See `lab.jpahibernate.entityidentity.UserAccount` and `lab.jpahibernate.entityidentity.AccountRegistryService`.
