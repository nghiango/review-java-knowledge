package lab.jpahibernate.broken.eagerfetching;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id private String id;
    private String projectName;

    public Project() {}

    public Project(String id, String projectName) {
        this.id = id;
        this.projectName = projectName;
    }

    public String getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }
}
