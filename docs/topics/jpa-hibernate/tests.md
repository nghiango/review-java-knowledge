# JPA / Hibernate Testing Strategies

## Testing Architecture

Testing JPA and Hibernate applications requires balancing fast mock-based unit tests with slice tests (`@DataJpaTest`) and full Testcontainers PostgreSQL integration tests.

| Test Level | Scope | Speed | Infrastructure | Tooling |
|---|---|---|---|---|
| **Unit Tests** | Service logic, DTO mapping, entity identity | Fast (< 100ms) | In-memory / Mockito | JUnit 5, Mockito, AssertJ |
| **Data JPA Slices** | Query syntax, JPQL, entity relationships | Medium (~ 1-2s) | Testcontainers PostgreSQL | `@DataJpaTest`, Testcontainers |
| **Integration Tests** | Full transaction lifecycle, concurrency, batching | Thorough (~ 3-5s) | Testcontainers PostgreSQL | `@SpringBootTest`, Testcontainers |

---

## Unit Test Implementations

### 1. N+1 Queries & DTO Projections Test
```java
--8<-- "modules/08-jpa-hibernate/src/test/java/lab/jpahibernate/nplusone/OrderServiceTest.java"
```

---

### 2. Entity Identity (`equals` / `hashCode`) Test
```java
--8<-- "modules/08-jpa-hibernate/src/test/java/lab/jpahibernate/entityidentity/EntityIdentityTest.java"
```

---

### 3. Join Table Cascade Management Test
```java
--8<-- "modules/08-jpa-hibernate/src/test/java/lab/jpahibernate/cascademapping/EnrollmentServiceTest.java"
```

---

### 4. DTO Mapping & Security Boundary Test
```java
--8<-- "modules/08-jpa-hibernate/src/test/java/lab/jpahibernate/dtomapping/UserProfileServiceTest.java"
```
