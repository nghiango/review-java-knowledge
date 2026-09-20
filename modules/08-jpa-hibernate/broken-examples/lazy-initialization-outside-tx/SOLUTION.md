# Solution: Lazy Initialization Outside Transaction

## Annotated Code

### `CustomerExportService.java`
```java
package lab.jpahibernate.broken.lazyinit;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerExportService {

    private final EntityManager entityManager;

    public CustomerExportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Customer> loadCustomers() {
        return entityManager
                .createQuery("SELECT c FROM Customer c", Customer.class)
                .getResultList();
    }

    public String exportContractSummaryReport() {
        List<Customer> customers = loadCustomers();

        StringBuilder report = new StringBuilder();
        for (Customer customer : customers) {
            // Correctness issue: Accessing uninitialized lazy collection outside an active PersistenceContext / Transaction throws LazyInitializationException
            int count = customer.getContracts().size();
            report.append(customer.getName()).append(": ").append(count).append(" contracts\n");
        }
        return report.toString();
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Lazy Collection Access Outside Active Session | High | Correctness | `loadCustomers()` closes the `EntityManager` session when its transaction completes. Accessing `customer.getContracts()` in `exportContractSummaryReport()` triggers `org.hibernate.LazyInitializationException: could not initialize proxy - no Session`. |

---

## Remediation Strategy

1. **JOIN FETCH or EntityGraph in Repository Query:**
   Fetch the required associations eagerly in the query used by the report:
   ```java
   SELECT c FROM Customer c LEFT JOIN FETCH c.contracts
   ```
2. **DTO Projection:**
   Avoid loading managed entities when only aggregate metrics or specific fields are required. Query directly into a DTO:
   ```java
   SELECT new lab.jpahibernate.lazyinit.CustomerSummaryDto(c.id, c.name, COUNT(ct.id))
   FROM Customer c LEFT JOIN c.contracts ct GROUP BY c.id, c.name
   ```
3. **Extend Transaction Boundary:**
   If entity lifecycle management is truly required, keep the transaction active across the processing scope, avoiding reliance on anti-patterns such as OSIV (Open Session In View).
