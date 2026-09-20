package lab.databasesql.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class IndexingOptimizationRepositoryTest {

    private JdbcClient jdbcClient;
    private IndexingOptimizationRepository repository;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        repository = new IndexingOptimizationRepository(jdbcClient);
    }

    @Test
    @DisplayName("findItemsByOrderId delegates to parameterized query on order_id")
    void findItemsByOrderId_returnsItems() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<IndexingOptimizationRepository.OrderItemDto> mappedSpec =
                mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.query(IndexingOptimizationRepository.OrderItemDto.class))
                .thenReturn(mappedSpec);
        when(mappedSpec.list())
                .thenReturn(
                        List.of(
                                new IndexingOptimizationRepository.OrderItemDto(
                                        1L, 100L, "Laptop", 1, 1200.0)));

        List<IndexingOptimizationRepository.OrderItemDto> items =
                repository.findItemsByOrderId(100L);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).productName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("findRecentLogsByTenant executes equality-first sort query")
    void findRecentLogsByTenant_returnsLogs() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<IndexingOptimizationRepository.AuditLogDto> mappedSpec =
                mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.query(IndexingOptimizationRepository.AuditLogDto.class))
                .thenReturn(mappedSpec);
        when(mappedSpec.list())
                .thenReturn(
                        List.of(
                                new IndexingOptimizationRepository.AuditLogDto(
                                        1L, "tenant_a", "SUCCESS", Instant.now(), "LOGIN", "ok")));

        List<IndexingOptimizationRepository.AuditLogDto> logs =
                repository.findRecentLogsByTenant("tenant_a", Instant.now().minusSeconds(3600), 10);

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).tenantId()).isEqualTo("tenant_a");
    }
}
