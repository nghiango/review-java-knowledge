package lab.webflux.blockingjdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CorrectCustomerSummaryServiceTest {

    private JdbcClient jdbcClient;
    private CorrectCustomerSummaryService service;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        service = new CorrectCustomerSummaryService(jdbcClient);
    }

    @Test
    @DisplayName("Should successfully return customer summary offloaded to boundedElastic")
    void getCustomerSummary_returnsSummarySuccessfully() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<CorrectCustomerSummaryService.CustomerSummary> mappedSpec =
                mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(CorrectCustomerSummaryService.CustomerSummary.class))
                .thenReturn(mappedSpec);
        when(mappedSpec.single())
                .thenReturn(
                        new CorrectCustomerSummaryService.CustomerSummary(
                                "cust-1", "Alice", 1250.50));

        Mono<CorrectCustomerSummaryService.CustomerSummary> summaryMono =
                service.getCustomerSummary("cust-1");

        StepVerifier.create(summaryMono)
                .assertNext(
                        summary -> {
                            assertThat(summary.customerId()).isEqualTo("cust-1");
                            assertThat(summary.name()).isEqualTo("Alice");
                            assertThat(summary.balance()).isEqualTo(1250.50);
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should emit error when underlying database query fails")
    void getCustomerSummary_emitsErrorOnDatabaseFailure() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), any())).thenReturn(statementSpec);
        when(statementSpec.query(CorrectCustomerSummaryService.CustomerSummary.class))
                .thenThrow(new IllegalStateException("Database connection pool exhausted"));

        Mono<CorrectCustomerSummaryService.CustomerSummary> summaryMono =
                service.getCustomerSummary("cust-error");

        StepVerifier.create(summaryMono).expectError(IllegalStateException.class).verify();
    }
}
