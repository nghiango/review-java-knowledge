package lab.databasesql.transactionscope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class InvoiceProcessingServiceTest {

    private JdbcClient jdbcClient;
    private InvoiceProcessingService service;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        service = new InvoiceProcessingService(jdbcClient);
    }

    @Test
    @DisplayName("claimInvoiceForProcessing updates status to PROCESSING")
    void claimInvoiceForProcessing_returnsTrueWhenClaimed() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        boolean claimed = service.claimInvoiceForProcessing(100L);

        assertThat(claimed).isTrue();
    }

    @Test
    @DisplayName("processInvoiceLifecycle runs full lifecycle orchestration")
    void processInvoiceLifecycle_claimsAndCompletes() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        service.processInvoiceLifecycle(100L);

        verify(jdbcClient)
                .sql(
                        "UPDATE invoices SET status = 'PROCESSING' WHERE id = :id AND status = 'PENDING'");
    }
}
