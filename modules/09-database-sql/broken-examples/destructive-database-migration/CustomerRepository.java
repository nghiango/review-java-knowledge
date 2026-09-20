package lab.databasesql.broken.migrations;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbcClient;

    public CustomerRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record CustomerRecord(Long id, String name, String fullAddress) {}

    public Optional<CustomerRecord> findCustomerById(Long id) {
        return jdbcClient
                .sql("SELECT id, name, full_address FROM customers WHERE id = :id")
                .param("id", id)
                .query(CustomerRecord.class)
                .optional();
    }
}
