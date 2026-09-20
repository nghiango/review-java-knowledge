package lab.webflux.blockingjdbc;

import java.time.Duration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class CorrectCustomerSummaryService {

    private final JdbcClient jdbcClient;

    public CorrectCustomerSummaryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Mono<CustomerSummary> getCustomerSummary(String customerId) {
        // Offload blocking JDBC queries to Schedulers.boundedElastic() to protect Netty event loops
        return Mono.fromCallable(
                        () -> {
                            return jdbcClient
                                    .sql(
                                            "SELECT customer_id, name, balance FROM customers WHERE customer_id = :id")
                                    .param("id", customerId)
                                    .query(CustomerSummary.class)
                                    .single();
                        })
                .subscribeOn(Schedulers.boundedElastic())
                .timeout(Duration.ofSeconds(3));
    }

    public record CustomerSummary(String customerId, String name, double balance) {}
}
