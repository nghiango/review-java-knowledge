package lab.jpahibernate.lazyfetching;

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

    /** Loads department names only without touching unneeded employee or project tables. */
    @Transactional(readOnly = true)
    public List<String> listDepartmentNames() {
        return entityManager
                .createQuery("SELECT d.name FROM Department d", String.class)
                .getResultList();
    }

    /** DTO projection summarizes counts without creating cartesian products in memory. */
    @Transactional(readOnly = true)
    public List<DepartmentSummaryDto> getDepartmentSummaries() {
        return entityManager
                .createQuery(
                        "SELECT new lab.jpahibernate.lazyfetching.DepartmentSummaryDto("
                                + "d.id, d.name, CAST(COUNT(DISTINCT e.id) AS int), CAST(COUNT(DISTINCT p.id) AS int)) "
                                + "FROM Department d "
                                + "LEFT JOIN d.employees e "
                                + "LEFT JOIN d.projects p "
                                + "GROUP BY d.id, d.name",
                        DepartmentSummaryDto.class)
                .getResultList();
    }
}
