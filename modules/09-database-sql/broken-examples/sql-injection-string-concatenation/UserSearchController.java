package lab.databasesql.broken.safequeries;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserSearchController {

    private final JdbcClient jdbcClient;

    public UserSearchController(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record UserRecord(Long id, String username, String email, String status) {}

    public List<UserRecord> searchUsers(String queryParam, String status) {
        // String concatenation directly embeds unsanitized user inputs into SQL string
        String sql =
                "SELECT id, username, email, status FROM users "
                        + "WHERE username LIKE '%"
                        + queryParam
                        + "%' AND status = '"
                        + status
                        + "'";

        return jdbcClient.sql(sql).query(UserRecord.class).list();
    }
}
