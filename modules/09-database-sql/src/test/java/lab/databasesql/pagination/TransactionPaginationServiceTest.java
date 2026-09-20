package lab.databasesql.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class TransactionPaginationServiceTest {

    private JdbcClient jdbcClient;
    private TransactionPaginationService paginationService;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        paginationService = new TransactionPaginationService(jdbcClient);
    }

    @Test
    @DisplayName("getTransactionsKeyset returns page and next cursor when hasMore is true")
    void getTransactionsKeyset_returnsPageAndNextCursor() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<TransactionPaginationService.TransactionRecord> mappedSpec =
                mock(JdbcClient.MappedQuerySpec.class);

        Instant now = Instant.now();
        List<TransactionPaginationService.TransactionRecord> results =
                List.of(
                        new TransactionPaginationService.TransactionRecord(
                                2L, "ACC_1", BigDecimal.valueOf(100), "COMPLETED", now),
                        new TransactionPaginationService.TransactionRecord(
                                1L,
                                "ACC_1",
                                BigDecimal.valueOf(50),
                                "COMPLETED",
                                now.minusSeconds(10)));

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.query(TransactionPaginationService.TransactionRecord.class))
                .thenReturn(mappedSpec);
        when(mappedSpec.list()).thenReturn(results);

        // Requested limit = 1, fetchSize = 2 (got 2 results -> hasMore = true)
        TransactionPaginationService.KeysetPage<TransactionPaginationService.TransactionRecord>
                page = paginationService.getTransactionsKeyset("ACC_1", null, 1);

        assertThat(page.items()).hasSize(1);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isNotNull();
        assertThat(page.nextCursor().id()).isEqualTo(2L);
    }
}
