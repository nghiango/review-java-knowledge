package lab.jpahibernate.lazyfetching;

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

class CompanyDirectoryServiceTest {

    private EntityManager entityManager;
    private CompanyDirectoryService service;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        service = new CompanyDirectoryService(entityManager);
    }

    @Test
    @DisplayName("listDepartmentNames queries department names only")
    void listDepartmentNames_queriesNamesOnly() {
        @SuppressWarnings("unchecked")
        TypedQuery<String> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(String.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of("Engineering", "Product"));

        List<String> names = service.listDepartmentNames();

        assertThat(names).containsExactly("Engineering", "Product");
        verify(entityManager).createQuery("SELECT d.name FROM Department d", String.class);
    }

    @Test
    @DisplayName("getDepartmentSummaries returns DTO projection with counts")
    void getDepartmentSummaries_returnsAggregatedDto() {
        @SuppressWarnings("unchecked")
        TypedQuery<DepartmentSummaryDto> query = mock(TypedQuery.class);
        DepartmentSummaryDto dto = new DepartmentSummaryDto(1L, "Engineering", 10, 3);
        when(entityManager.createQuery(anyString(), eq(DepartmentSummaryDto.class)))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(dto));

        List<DepartmentSummaryDto> summaries = service.getDepartmentSummaries();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).name()).isEqualTo("Engineering");
        assertThat(summaries.get(0).employeeCount()).isEqualTo(10);
        assertThat(summaries.get(0).projectCount()).isEqualTo(3);
    }
}
