package lab.databasesql.safequeries;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerSearchService {

    private final JdbcClient jdbcClient;

    public CustomerSearchService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record UserRecord(Long id, String username, String email, String status) {}

    /**
     * Parameterized queries completely prevent SQL injection and enable DB PreparedStatement
     * caching.
     */
    public List<UserRecord> searchUsersSafe(String queryParam, String status) {
        String likePattern = "%" + (queryParam == null ? "" : queryParam.trim()) + "%";
        return jdbcClient
                .sql(
                        "SELECT id, username, email, status "
                                + "FROM users "
                                + "WHERE username LIKE :likePattern AND status = :status")
                .param("likePattern", likePattern)
                .param("status", status)
                .query(UserRecord.class)
                .list();
    }
}
