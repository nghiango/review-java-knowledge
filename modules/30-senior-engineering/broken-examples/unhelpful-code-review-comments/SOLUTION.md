# Solution: Review of Unhelpful Code Review Comments

## Annotated Artifact

```markdown
# PR #249 Review Comments: Add Customer Search API

**Author:** Junior Backend Engineer  
**Reviewer:** Senior Backend Engineer  

---

### Code Diff under Review

```java
// File: src/main/java/com/example/customer/CustomerService.java

public class CustomerService {

    // Maintainability issue: Field injection with @Autowired instead of constructor injection; hinders testing
    @Autowired
    private DataSource dataSource;

    // Concurrency issue: Unsynchronized HashMap shared across concurrent HTTP request threads (causes race conditions, corrupt buckets, CPU 100%)
    // Reliability issue: Unbounded in-memory cache without TTL or maximum size policy will trigger OutOfMemoryError
    private Map<String, Customer> cache = new HashMap<>();

    // Maintainability issue: Broad 'throws Exception' hides specific failure contracts from callers
    public Customer findCustomer(String queryParam) throws Exception {
        if (cache.containsKey(queryParam)) {
            return cache.get(queryParam);
        }

        // Reliability issue: Unclosed database Connection, Statement, and ResultSet leaking pooled connections and exhausting HikariCP
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        // Security issue: Catastrophic SQL Injection via raw string concatenation with untrusted user input
        String sql = "SELECT * FROM customers WHERE email = '" + queryParam + "'";
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            Customer c = new Customer(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
            cache.put(queryParam, c);
            return c;
        }
        return null;
    }
}
```

---

### Submitted Review Comments

**Reviewer Comment 1 (Line 5):**
> Why are you calling this `CustomerService`? The name is completely unoriginal and generic. Rename it to `CustomerSearchQueryFacadeHandler`.
<!-- Maintainability issue: Bikeshedding (Parkinson's Law of Triviality); demanding needlessly complex names based on subjective personal preference without rationale -->

**Reviewer Comment 2 (Line 10):**
> Bad indentation. You have 4 spaces here instead of 2. Fix your formatting. Did you even bother running the formatter before pinging me?
<!-- Maintainability issue: Hostile, condescending tone eroding psychological safety; formatting debates should be enforced mechanically by Spotless/Checkstyle in CI, never by humans in review -->

**Reviewer Comment 3 (Line 12):**
> `queryParam` is an ugly variable name. Change it to `inputParameterString`.
<!-- Maintainability issue: Low-value nitpick proposing an inferior, overly verbose Hungarian-style name -->

**Reviewer Comment 4 (Line 24):**
> You are using `c` as a temporary variable name. Never use single-letter variables in Java, it looks like amateur code. Change it to `instantiatedCustomerDomainObject`.
<!-- Maintainability issue: Insulting phrasing ("amateur code") coupled with absurd over-engineering for a 3-line scoped local variable -->

**Reviewer Comment 5 (PR Summary):**
> Requesting changes. This code is messy. Clean up the variable names and indentation as noted above, then I might approve it.
<!-- Security issue: Reviewer missed a critical SQL injection and leak vulnerability that would have shipped to production with reviewer sign-off -->
<!-- Maintainability issue: Withholding approval conditionally ("I might approve it") establishes an adversarial dynamic rather than collaborative peer review -->
```

## Issue Analysis

### 1. The Perils of Bikeshedding (Parkinson's Law of Triviality)
- **Problem**: The reviewer focused 100% of their attention on indentation, spacing, and personal naming preferences, while completely overlooking catastrophic defects:
  - **SQL Injection**: An attacker passing `' OR 1=1 --` exfiltrates the entire database.
  - **Connection Leak**: Opening raw JDBC connections without `try-with-resources` exhausts HikariCP within 20 requests.
  - **Concurrency Race Condition**: Non-thread-safe `HashMap` accessed concurrently will corrupt internal bucket nodes or spin in infinite loops during rehashing.
- **Rule of Thumb**: Automate everything mechanical (Spotless, Error Prone, SonarQube). Reserve human review bandwidth for architecture, security boundaries, edge cases, and business logic.

### 2. Destructive Communication & Psychological Safety
- **Problem**: Condescending comments ("Did you even bother running the formatter before pinging me?", "looks like amateur code") trigger defensiveness, shame, and hesitation to ask questions.
- **Correction**: Adopt **Conventional Comments** (`blocking:`, `suggestion:`, `question:`, `nit:`). Explain *why* a change is requested, cite documentation or security risks, and offer clear code examples.

---

## Correct Implementation

See full constructive code review in [correct/pr-review-comments-constructive.md](correct/pr-review-comments-constructive.md).
