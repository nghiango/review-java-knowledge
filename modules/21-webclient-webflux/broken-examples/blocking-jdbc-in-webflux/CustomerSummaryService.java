package lab.webflux.broken.blockingjdbc;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomerSummaryService {

  private final JdbcClient jdbcClient;

  public CustomerSummaryService(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public Mono<CustomerSummary> getCustomerSummary(String customerId) {
    // Calling blocking JDBC directly inside Mono.fromSupplier
    return Mono.fromSupplier(() -> {
      return jdbcClient.sql("SELECT customer_id, name, balance FROM customers WHERE customer_id = :id")
          .param("id", customerId)
          .query(CustomerSummary.class)
          .single();
    });
  }

  public record CustomerSummary(String customerId, String name, double balance) {}
}
