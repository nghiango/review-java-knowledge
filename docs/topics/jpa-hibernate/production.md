# JPA / Hibernate in Production

## 1. Real-World Incident Postmortems

### Incident 1: Black Friday N+1 Cascade Outage
- **Symptom:** Database CPU reached 100%, and connection pool wait times spiked to 30 seconds during an e-commerce flash sale.
- **Root Cause:** A new catalog summary endpoint loaded 5,000 products and looped through `product.getInventoryItems()`. Because `inventoryItems` was lazily loaded, the application executed 5,001 individual SQL queries per request. At 200 requests/second, the database was hit with over 1,000,000 queries per second.
- **Remediation:** Replaced entity iteration with a DTO projection query:
  ```java
  SELECT new lab.jpahibernate.nplusone.ProductInventoryDto(p.id, p.name, SUM(i.availableStock))
  FROM Product p LEFT JOIN p.inventoryItems i GROUP BY p.id, p.name
  ```
- **Prevention:** Integrated `datasource-proxy` and automated assertion tests (`assertMaxQueryCount(1)`) in CI test suites.

---

### Incident 2: HikariCP Starvation via Open Session in View (OSIV)
- **Symptom:** API gateway reported 504 Gateway Timeouts on checkout endpoints during peak traffic.
- **Root Cause:** Spring Boot's default `spring.jpa.open-in-view=true` kept database connections open during HTTP response streaming and slow third-party payment gateway calls. HikariCP pool (10 connections) was exhausted within seconds.
- **Remediation:** Disabled OSIV (`spring.jpa.open-in-view=false`) across all microservices and restricted database transactions strictly to the service layer.

---

### Incident 3: Cartesian Product Heap Exhaustion (`OutOfMemoryError`)
- **Symptom:** Batch reporting worker crashed with `java.lang.OutOfMemoryError: Java heap space`.
- **Root Cause:** A query used multiple `JOIN FETCH` clauses across two `@OneToMany` collection associations (`department.getEmployees()` and `department.getProjects()`). For a department with 500 employees and 100 projects, the query generated $500 \times 100 = 50,000$ duplicate rows in memory.
- **Remediation:** Replaced multi-bag `JOIN FETCH` with separate queries using `@BatchSize` or two sequential queries.

---

## 2. Production Telemetry and Monitoring

### Effective Logging Configuration
Avoid `spring.jpa.show-sql=true` in production (it writes directly to `System.out` without timestamps or parameters). Instead, use structured SQL logging via SLF4J:

```properties
# Enable SQL statement logging
logging.level.org.hibernate.SQL=DEBUG
# Log bind parameters
logging.level.org.hibernate.orm.jdbc.bind=TRACE
# Log statement execution time
logging.level.org.hibernate.SQL_SLOW=WARN
# Threshold for slow query warnings (ms)
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=200
```

### Metrics with Micrometer & Actuator
Enable Hibernate statistics to monitor query and cache performance in Grafana:

```properties
spring.jpa.properties.hibernate.generate_statistics=true
```

Key Metrics:
- `hibernate.entities.loads` — Total entity load count (detects N+1 query storms)
- `hibernate.statements` — Total executed JDBC statements
- `hibernate.query.executions` — Count and execution duration of JPQL/Criteria queries
- `hibernate.second.level.cache.hit` / `miss` — L2 cache hit ratios

---

## 3. Production Hardening Checklist

- [ ] Set `spring.jpa.open-in-view=false` in `application.properties`.
- [ ] Explicitly configure `fetch = FetchType.LAZY` on all `@ManyToOne` and `@OneToOne` associations.
- [ ] Ensure all `@Entity` classes implement proxy-safe `equals()` and `hashCode()` using immutable business keys.
- [ ] Enable JDBC batching (`batch_size=50`, `order_inserts=true`, `order_updates=true`).
- [ ] Use `GenerationType.SEQUENCE` instead of `IDENTITY` for high-volume entities.
- [ ] Add foreign key indexes on all `@JoinColumn` fields in database migrations (Flyway / Liquibase).
- [ ] Use `StatelessSession` or periodic `em.clear()` for batch ETL jobs.
