package lab.databasesql.safequeries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class CustomerSearchServiceTest {

    private JdbcClient jdbcClient;
    private CustomerSearchService searchService;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        searchService = new CustomerSearchService(jdbcClient);
    }

    @Test
    @DisplayName("searchUsersSafe binds parameters securely")
    void searchUsersSafe_bindsParams() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<CustomerSearchService.UserRecord> mappedSpec =
                mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.query(CustomerSearchService.UserRecord.class)).thenReturn(mappedSpec);
        when(mappedSpec.list())
                .thenReturn(
                        List.of(
                                new CustomerSearchService.UserRecord(
                                        1L, "alice", "alice@example.com", "ACTIVE")));

        List<CustomerSearchService.UserRecord> results =
                searchService.searchUsersSafe("alice", "ACTIVE");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).username()).isEqualTo("alice");
    }
}
