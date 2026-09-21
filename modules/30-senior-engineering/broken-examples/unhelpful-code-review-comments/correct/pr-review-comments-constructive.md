# Exemplary Constructive Code Review: PR #249

**Reviewer:** Senior / Staff Backend Engineer  
**PR:** #249 — Add Customer Search API  
**Status:** Request Changes (Blocking security & reliability vulnerabilities)  

---

### PR Summary Review

> Thanks for submitting this search implementation! The domain model and endpoint routing look very clean.
> 
> Before we can merge this to staging, there are two **blocking security and reliability items** we need to address:
> 1. **SQL Injection Hazard**: Using raw string concatenation in SQL queries leaves the database vulnerable to injection. We can resolve this immediately by switching to Spring's parameterized `JdbcClient` or `PreparedStatement`.
> 2. **Connection Leak & Concurrency Hazard**: The current code manually opens raw JDBC connections without closing them, and uses an unsynchronized `HashMap` across concurrent web requests.
> 
> I've left detailed inline comments with suggested code snippets below to show how we can modernize this with Spring Data or `JdbcClient`. Happy to hop on a quick 10-minute pairing session if you'd like to walk through it together!

---

### Inline Review Comments

#### Comment 1 (Line 8: Dependency Injection)
> **suggestion:** Prefer constructor injection over `@Autowired` on private fields.  
> 
> *Why:* Constructor injection ensures the service is immutable (`final` fields) and makes it trivial to instantiate in fast slice or unit tests without needing a Spring container.
> 
> ```java
> @Service
> public class CustomerService {
>     private final JdbcClient jdbcClient;
> 
>     public CustomerService(JdbcClient jdbcClient) {
>         this.jdbcClient = jdbcClient;
>     }
> ```

#### Comment 2 (Line 10: In-Memory Caching)
> **blocking:** `HashMap` is not thread-safe for concurrent read/write operations and can cause CPU spikes or data corruption under web traffic. Additionally, an unbounded map can lead to an `OutOfMemoryError`.
> 
> *Why:* Multiple HTTP requests execute concurrently. If two threads mutate a standard `HashMap` simultaneously, it can throw `ConcurrentModificationException` or corrupt internal bucket pointers.
> 
> *Recommendation:* If we need caching here, let's use Spring's `@Cacheable("customers")` backed by Caffeine with an explicit maximum size and TTL (e.g. 10 minutes), or a `ConcurrentHashMap` with bounded eviction. If traffic is low, we can even query the database directly first with a fast index on `email`.

#### Comment 3 (Line 15–20: SQL Injection & Connection Leak)
> **blocking:** Raw SQL string concatenation (`"WHERE email = '" + queryParam + "'"` ) introduces a critical SQL injection vulnerability (OWASP Top 10). Furthermore, `conn`, `stmt`, and `rs` are not closed in a `try-with-resources` block, which will exhaust our HikariCP connection pool under load.
> 
> *Why:* An input like `admin@example.com' OR '1'='1` can leak customer records. Also, any exception thrown during query execution will bypass `.close()`, permanently leaking database connections.
> 
> *Recommended Fix:* We can leverage Spring 6 / Boot 3's modern `JdbcClient`, which handles connection acquisition, parameterized query binding, result mapping, and automatic connection release:
> 
> ```java
> public Optional<Customer> findCustomerByEmail(String email) {
>     return jdbcClient.sql("SELECT id, name, email FROM customers WHERE email = :email")
>             .param("email", email)
>             .query(Customer.class)
>             .optional();
> }
> ```

#### Comment 4 (Line 12: Naming & Signatures)
> **suggestion:** Consider renaming `queryParam` to `email` and returning `Optional<Customer>` instead of `Customer` with `null`.
> 
> *Why:* Since this query specifically looks up users by email, `email` communicates intent clearly to callers. Returning `Optional<Customer>` makes the absence of a customer explicit at compile time, protecting callers from accidental `NullPointerException`s.

#### Comment 5 (Line 10: Formatting)
> **nit:** Our repository uses Spotless for automatic formatting. You can run `./gradlew spotlessApply` in your terminal to automatically format all files before committing!
