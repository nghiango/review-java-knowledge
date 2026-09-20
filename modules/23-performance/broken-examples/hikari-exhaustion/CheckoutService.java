package lab.performance.broken.connectionpool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public final class CheckoutService {
    private final DataSource dataSource;
    private final PaymentGateway paymentGateway;

    public CheckoutService(DataSource dataSource, PaymentGateway paymentGateway) {
        this.dataSource = dataSource;
        this.paymentGateway = paymentGateway;
    }

    public void checkout(long orderId) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement =
                    connection.prepareStatement("update orders set status = 'PAYING' where id = ?")) {
                statement.setLong(1, orderId);
                statement.executeUpdate();
            }
            paymentGateway.charge(orderId);
            connection.commit();
        }
    }

    public interface PaymentGateway {
        void charge(long orderId);
    }
}
