package lab.jpahibernate.lazyinit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerExportServiceTest {

    private EntityManager entityManager;
    private CustomerExportService service;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        service = new CustomerExportService(entityManager);
    }

    @Test
    @DisplayName("loadCustomersWithContractsJoinFetch queries with JOIN FETCH")
    void loadCustomersWithContractsJoinFetch_usesJoinFetch() {
        @SuppressWarnings("unchecked")
        TypedQuery<Customer> query = mock(TypedQuery.class);
        Customer customer = new Customer("Acme Corp");
        customer.addContract(new Contract("CTR-1001"));

        when(entityManager.createQuery(anyString(), eq(Customer.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(customer));

        List<Customer> result = service.loadCustomersWithContractsJoinFetch();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContracts()).hasSize(1);
        verify(entityManager)
                .createQuery(
                        "SELECT DISTINCT c FROM Customer c LEFT JOIN FETCH c.contracts",
                        Customer.class);
    }

    @Test
    @DisplayName("exportContractSummaryReport generates report safely using DTOs")
    void exportContractSummaryReport_generatesReport() {
        @SuppressWarnings("unchecked")
        TypedQuery<CustomerSummaryDto> query = mock(TypedQuery.class);
        CustomerSummaryDto dto = new CustomerSummaryDto(1L, "Acme Corp", 3);

        when(entityManager.createQuery(anyString(), eq(CustomerSummaryDto.class)))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dto));

        String report = service.exportContractSummaryReport();

        assertThat(report).contains("Acme Corp: 3 contracts");
    }
}
