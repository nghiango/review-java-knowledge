package lab.jpahibernate.broken.eagerfetching;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyDirectoryService {

    private final EntityManager entityManager;

    public CompanyDirectoryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<String> listDepartmentNames() {
        // Query intends to fetch ONLY department names, but EAGER fetching triggers joins across employees, projects, etc.
        List<Department> departments =
                entityManager
                        .createQuery("SELECT d FROM Department d", Department.class)
                        .getResultList();

        return departments.stream().map(Department::getDepartmentName).toList();
    }
}
