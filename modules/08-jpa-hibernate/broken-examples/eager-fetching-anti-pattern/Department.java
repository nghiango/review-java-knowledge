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

    // EAGER fetch type loads all employees and their eager projects across the entire graph
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
