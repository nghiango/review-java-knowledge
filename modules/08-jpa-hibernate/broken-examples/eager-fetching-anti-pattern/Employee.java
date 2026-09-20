package lab.jpahibernate.broken.eagerfetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id private String id;
    private String name;
    private String title;

    // EAGER fetch type on collection forces automatic joining and memory loading on every query
    @ManyToMany(fetch = FetchType.EAGER)
    private List<Project> projects = new ArrayList<>();

    public Employee() {}

    public Employee(String id, String name, String title, List<Project> projects) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.projects = projects;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public List<Project> getProjects() {
        return projects;
    }
}
