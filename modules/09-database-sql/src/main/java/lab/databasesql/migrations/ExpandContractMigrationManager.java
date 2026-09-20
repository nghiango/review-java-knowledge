package lab.databasesql.migrations;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpandContractMigrationManager {

    private final JdbcClient jdbcClient;

    public ExpandContractMigrationManager(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record CustomerDto(Long id, String name, String deliveryAddress) {}

    /**
     * Phase 1 & 2: Read with backward compatibility fallback (COALESCE between new and old
     * columns).
     */
    public Optional<CustomerDto> findCustomerCompatible(Long id) {
        return jdbcClient
                .sql(
                        "SELECT id, name, COALESCE(delivery_address, full_address) AS delivery_address "
                                + "FROM customers WHERE id = :id")
                .param("id", id)
                .query(CustomerDto.class)
                .optional();
    }

    /**
     * Phase 2 (Dual Write): Application writes to both columns to ensure zero downtime for old
     * instances.
     */
    @Transactional
    public void updateCustomerDualWrite(Long id, String newAddress) {
        jdbcClient
                .sql(
                        "UPDATE customers "
                                + "SET delivery_address = :addr, full_address = :addr "
                                + "WHERE id = :id")
                .param("addr", newAddress)
                .param("id", id)
                .update();
    }
}
