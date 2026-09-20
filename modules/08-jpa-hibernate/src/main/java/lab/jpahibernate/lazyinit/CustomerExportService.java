package lab.jpahibernate.lazyinit;

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

    /**
     * Fix 1: JOIN FETCH initializes the lazy relationship inside the transaction boundary, so that
     * callers outside the transaction can safely access getContracts().
     */
    @Transactional(readOnly = true)
    public List<Customer> loadCustomersWithContractsJoinFetch() {
        return entityManager
                .createQuery(
                        "SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.contracts",
                        Customer.class)
                .getResultList();
    }

    /**
     * Fix 2: DTO Projection selects aggregate data directly, avoiding entity detached state
     * altogether.
     */
    @Transactional(readOnly = true)
    public List<CustomerSummaryDto> loadCustomerSummariesDto() {
        return entityManager
                .createQuery(
                        "SELECT new lab.jpahibernate.lazyinit.CustomerSummaryDto("
                                + "c.id, c.name, CAST(COUNT(ct.id) AS int)) "
                                + "FROM Customer c LEFT JOIN c.contracts ct "
                                + "GROUP BY c.id, c.name",
                        CustomerSummaryDto.class)
                .getResultList();
    }

    public String exportContractSummaryReport() {
        List<CustomerSummaryDto> summaries = loadCustomerSummariesDto();
        StringBuilder report = new StringBuilder();
        for (CustomerSummaryDto summary : summaries) {
            report.append(summary.name())
                    .append(": ")
                    .append(summary.contractCount())
                    .append(" contracts\n");
        }
        return report.toString();
    }
}
