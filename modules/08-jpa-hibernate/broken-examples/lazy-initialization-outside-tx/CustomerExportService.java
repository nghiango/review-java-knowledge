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
            int count = customer.getContracts().size();
            report.append(customer.getName()).append(": ").append(count).append(" contracts\n");
        }
        return report.toString();
    }
}
