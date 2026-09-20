# Solution: Eager Fetching Anti-Pattern

## Annotated Code

```java
package lab.jpahibernate.broken.eagerfetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
public class Department {

    @Id private String id;
    private String departmentName;

    // Performance issue: FetchType.EAGER loads all child employees and nested associations unconditionally on every query
    @OneToMany(fetch = FetchType.EAGER)
    private List<Employee> employees = new ArrayList<>();

    public Department() {}

    public Department(String id, String departmentName, List<Employee> employees) {
        this.id = id;
        this.departmentName = departmentName;
        this.employees = employees;
    }

    public String getId() {
        return id;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public List<Employee> getEmployees() {
        return employees;
    }
}
```

## Issues Identified

### Performance issue: FetchType.EAGER forces join cascades and cannot be overridden at query time
- **Location:** `Department.java#employees`, `Employee.java#projects`
- **Explanation:** `FetchType.EAGER` mapping on `@OneToMany` and `@ManyToMany` instructs Hibernate to load the entire object graph eagerly on every single query (e.g. `SELECT d FROM Department d` or `findById()`). This creates massive Cartesian product joins, exhausts JVM heap memory, and causes severe query latency. Furthermore, JPQL queries cannot dynamically disable `EAGER` mappings. All associations should default to `FetchType.LAZY` and use explicit query-level fetching (`JOIN FETCH` or `@EntityGraph`) only when child data is genuinely required.

## Correct implementation

See `lab.jpahibernate.lazyfetching.Department`, `lab.jpahibernate.lazyfetching.Employee`, and `lab.jpahibernate.lazyfetching.CompanyDirectoryService`.
