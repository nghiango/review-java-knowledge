# JPA / Hibernate Code Review Challenges

This section contains 7 real-world pull requests with subtle ORM bugs, performance bottlenecks, and architectural anti-patterns. Review the code as a senior engineer before inspecting the solutions.

---

## 1. N+1 Queries from Lazy Iteration

### Context
An order management service generates summary reports. Each `Order` has a `@OneToMany(fetch = FetchType.LAZY)` relationship to `OrderItem`. The service loads all orders and loops over them to compute metrics.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/n-plus-one-queries/Order.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/n-plus-one-queries/OrderItem.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/n-plus-one-queries/OrderSummaryService.java"
```

### Review Questions
1. How many total SQL queries are executed when `calculateOrderSummaries()` processes 1,000 orders?
2. What are the two best production-grade fixes for this issue?

---

## 2. Eager Fetching Anti-Pattern

### Context
A corporate directory service models `Department`, `Employee`, and `Project`. The developer added `@OneToMany(fetch = FetchType.EAGER)` and `@ManyToOne(fetch = FetchType.EAGER)` across all entities to prevent `LazyInitializationException`.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/eager-fetching-anti-pattern/Department.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/eager-fetching-anti-pattern/Employee.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/eager-fetching-anti-pattern/Project.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/eager-fetching-anti-pattern/CompanyDirectoryService.java"
```

### Review Questions
1. What query complexity and memory overhead occurs when simply listing department names?
2. What happens when multiple eager collection associations are fetched simultaneously?

---

## 3. Equals and HashCode with Generated ID

### Context
An account registry maintains a registry of active user accounts in an in-memory `Set`. The `UserAccount` entity implements `equals()` and `hashCode()` using its auto-generated primary key `id`.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/equals-hashcode-generated-id/UserAccount.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/equals-hashcode-generated-id/AccountRegistryService.java"
```

### Review Questions
1. Why does `activeAccounts.contains(account)` return `false` immediately after persisting the account?
2. How should `equals()` and `hashCode()` be implemented in JPA entities?

---

## 4. Bidirectional JSON Recursion

### Context
A book catalog REST controller returns managed `Author` and `Book` entities directly in HTTP responses. The entities declare bidirectional `@ManyToOne` and `@OneToMany` relationships.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/bidirectional-json-recursion/Author.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/bidirectional-json-recursion/Book.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/bidirectional-json-recursion/BookCatalogController.java"
```

### Review Questions
1. What exception occurs when Jackson serializes the `Author` or `Book` entity response?
2. Why is using DTO projections preferred over Jackson annotations (`@JsonManagedReference`, `@JsonIgnore`) on JPA entities?

---

## 5. Lazy Initialization Outside Transaction

### Context
A customer export service loads customers in a transactional helper method, but aggregates contract metrics in a non-transactional export method.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/lazy-initialization-outside-tx/Customer.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/lazy-initialization-outside-tx/Contract.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/lazy-initialization-outside-tx/CustomerExportService.java"
```

### Review Questions
1. Why does `customer.getContracts().size()` throw `LazyInitializationException`?
2. Why is enabling Open Session in View (OSIV) not recommended as a fix?

---

## 6. Cascade ALL on ManyToMany Relationship

### Context
A university portal manages student registrations for courses. The developer applied `cascade = CascadeType.ALL` to the `@ManyToMany` association between `Student` and `Course`.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/cascade-all-many-to-many/Student.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/cascade-all-many-to-many/Course.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/cascade-all-many-to-many/EnrollmentService.java"
```

### Review Questions
1. What catastrophic data loss occurs when `unregisterStudent(studentId)` executes?
2. How should many-to-many relationships and join tables be modeled safely?

---

## 7. Entity Exposed Through API

### Context
A user profile controller accepts and returns the `UserProfile` JPA entity directly in its REST endpoints.

### Clean Code Under Review

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/entity-exposed-through-api/UserProfile.java"
```

```java
--8<-- "modules/08-jpa-hibernate/broken-examples/entity-exposed-through-api/UserProfileController.java"
```

### Review Questions
1. What security vulnerabilities (Mass Assignment, Sensitive Data Exposure) exist in this controller?
2. How does Hibernate dirty checking interact with directly bound request bodies?
