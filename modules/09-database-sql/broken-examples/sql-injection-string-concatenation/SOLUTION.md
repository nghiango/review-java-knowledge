# Solution: SQL Injection via String Concatenation

## Annotated Code

### `UserSearchController.java`
```java
package lab.databasesql.broken.safequeries;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserSearchController {

    private final JdbcClient jdbcClient;

    public UserSearchController(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record UserRecord(Long id, String username, String email, String status) {}

    public List<UserRecord> searchUsers(String queryParam, String status) {
        // Security issue: String concatenation creates SQL Injection vulnerabilities (OWASP A03) and evicts database PreparedStatement execution caches
        String sql =
                "SELECT id, username, email, status FROM users "
                        + "WHERE username LIKE '%"
                        + queryParam
                        + "%' AND status = '"
                        + status
                        + "'";

        return jdbcClient.sql(sql).query(UserRecord.class).list();
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| SQL Injection Vulnerability | Critical | Security | Concatenating untrusted user input directly into SQL strings allows attackers to alter query logic, bypass authentication, extract sensitive records, or execute arbitrary SQL commands. |
| PreparedStatement Cache Invalidation | Medium | Performance | Every distinct input string produces a distinct SQL text, polluting the database query plan cache and requiring repeated query parsing and optimization. |

---

## Remediation Strategy

1. **Use Parameterized Bind Variables:**
   ```java
   public List<UserRecord> searchUsers(String queryParam, String status) {
       String likePattern = "%" + queryParam + "%";
       return jdbcClient
               .sql("SELECT id, username, email, status FROM users WHERE username LIKE :likePattern AND status = :status")
               .param("likePattern", likePattern)
               .param("status", status)
               .query(UserRecord.class)
               .list();
   }
   ```
