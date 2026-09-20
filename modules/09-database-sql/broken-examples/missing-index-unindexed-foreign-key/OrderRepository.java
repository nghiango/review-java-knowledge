package lab.databasesql.broken.indexing;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final JdbcClient jdbcClient;

    public OrderRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record OrderItemRecord(
            Long id, Long orderId, String productName, int quantity, double unitPrice) {}

    public List<OrderItemRecord> findItemsByOrderId(Long orderId) {
        return jdbcClient
                .sql(
                        "SELECT id, order_id, product_name, quantity, unit_price "
                                + "FROM order_items WHERE order_id = :orderId")
                .param("orderId", orderId)
                .query(OrderItemRecord.class)
                .list();
    }

    public void deleteOrder(Long orderId) {
        jdbcClient.sql("DELETE FROM orders WHERE id = :orderId").param("orderId", orderId).update();
    }
}
