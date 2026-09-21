# PR #249 Review Comments: Add Customer Search API

**Author:** Junior Backend Engineer  
**Reviewer:** Senior Backend Engineer  

---

### Code Diff under Review

```java
// File: src/main/java/com/example/customer/CustomerService.java

public class CustomerService {

    @Autowired
    private DataSource dataSource;

    private Map<String, Customer> cache = new HashMap<>();

    public Customer findCustomer(String queryParam) throws Exception {
        if (cache.containsKey(queryParam)) {
            return cache.get(queryParam);
        }

        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
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

**Reviewer Comment 2 (Line 10):**
> Bad indentation. You have 4 spaces here instead of 2. Fix your formatting. Did you even bother running the formatter before pinging me?

**Reviewer Comment 3 (Line 12):**
> `queryParam` is an ugly variable name. Change it to `inputParameterString`.

**Reviewer Comment 4 (Line 24):**
> You are using `c` as a temporary variable name. Never use single-letter variables in Java, it looks like amateur code. Change it to `instantiatedCustomerDomainObject`.

**Reviewer Comment 5 (PR Summary):**
> Requesting changes. This code is messy. Clean up the variable names and indentation as noted above, then I might approve it.
