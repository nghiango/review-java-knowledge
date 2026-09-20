package lab.databasesql.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class ExpandContractMigrationManagerTest {

    private JdbcClient jdbcClient;
    private ExpandContractMigrationManager migrationManager;

    @BeforeEach
    void setUp() {
        jdbcClient = mock(JdbcClient.class);
        migrationManager = new ExpandContractMigrationManager(jdbcClient);
    }

    @Test
    @DisplayName("findCustomerCompatible reads fallback column gracefully")
    void findCustomerCompatible_returnsCustomerDto() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        @SuppressWarnings("unchecked")
        JdbcClient.MappedQuerySpec<ExpandContractMigrationManager.CustomerDto> mappedSpec =
                mock(JdbcClient.MappedQuerySpec.class);

        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.query(ExpandContractMigrationManager.CustomerDto.class))
                .thenReturn(mappedSpec);
        when(mappedSpec.optional())
                .thenReturn(
                        Optional.of(
                                new ExpandContractMigrationManager.CustomerDto(
                                        1L, "Acme Corp", "123 Main St")));

        Optional<ExpandContractMigrationManager.CustomerDto> customer =
                migrationManager.findCustomerCompatible(1L);

        assertThat(customer).isPresent();
        assertThat(customer.get().deliveryAddress()).isEqualTo("123 Main St");
    }

    @Test
    @DisplayName("updateCustomerDualWrite updates both columns in dual write phase")
    void updateCustomerDualWrite_updatesBothColumns() {
        JdbcClient.StatementSpec statementSpec = mock(JdbcClient.StatementSpec.class);
        when(jdbcClient.sql(anyString())).thenReturn(statementSpec);
        when(statementSpec.param(anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(statementSpec);
        when(statementSpec.update()).thenReturn(1);

        migrationManager.updateCustomerDualWrite(1L, "456 Market St");

        verify(jdbcClient)
                .sql(
                        "UPDATE customers "
                                + "SET delivery_address = :addr, full_address = :addr "
                                + "WHERE id = :id");
    }
}
