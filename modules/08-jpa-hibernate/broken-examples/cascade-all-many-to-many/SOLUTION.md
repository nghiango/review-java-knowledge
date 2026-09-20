# Solution: Cascade All on ManyToMany Relationship

## Annotated Code

### `Student.java`
```java
package lab.jpahibernate.broken.cascademapping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Correctness issue: CascadeType.ALL includes CascadeType.REMOVE, which deletes shared Course records and breaks data integrity for other students enrolled in those courses
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "student_courses",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();

    protected Student() {}

    public Student(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<Course> getCourses() {
        return courses;
    }

    public void addCourse(Course course) {
        this.courses.add(course);
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| CascadeType.REMOVE on ManyToMany Association | Critical | Correctness | Using `CascadeType.ALL` or `CascadeType.REMOVE` on `@ManyToMany` causes JPA/Hibernate to delete the target entities (`Course`) when the source entity (`Student`) is deleted. In a shared relationship, courses enrolled in by multiple students will be deleted, cascading foreign key failures or corrupting shared catalog data. |

---

## Remediation Strategy

1. **Remove CascadeType.REMOVE / CascadeType.ALL from @ManyToMany:**
   `@ManyToMany` associations should typically use `CascadeType.PERSIST` / `CascadeType.MERGE` or no cascade at all.
2. **Explicit Join Table Management:**
   Removing a student should only delete the rows in `student_courses` join table, not the target `courses` table:
   ```java
   @ManyToMany
   @JoinTable(
       name = "student_courses",
       joinColumns = @JoinColumn(name = "student_id"),
       inverseJoinColumns = @JoinColumn(name = "course_id")
   )
   private Set<Course> courses = new HashSet<>();
   ```
3. **Explicit Join Entity with Additional Attributes:**
   In production domains, many-to-many relationships often have payload data (e.g. `enrolledAt`, `grade`, `status`). Map an explicit `@Entity` `Enrollment` with `@ManyToOne` to `Student` and `Course`.
