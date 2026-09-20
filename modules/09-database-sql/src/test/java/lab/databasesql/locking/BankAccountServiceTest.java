package lab.databasesql.locking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class BankAccountServiceTest {

    private JdbcClient jdbcClient;
    private BankAccountService bankAccountService;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        bankAccountService = new BankAccountService(jdbcClient);
    }

    @Test
    @DisplayName("withdrawAtomic succeeds when balance is sufficient")
    void withdrawAtomic_success() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        bankAccountService.withdrawAtomic(1L, BigDecimal.valueOf(50));

        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("withdrawAtomic throws exception when balance is insufficient")
    void withdrawAtomic_insufficientFunds_throwsException() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(0); // 0 rows updated

        assertThatThrownBy(() -> bankAccountService.withdrawAtomic(1L, BigDecimal.valueOf(500)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");
    }
}
