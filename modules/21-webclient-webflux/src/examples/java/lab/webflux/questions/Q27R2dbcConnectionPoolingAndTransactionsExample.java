package lab.webflux.questions;

import java.util.Map;

/**
 * Q27: How does R2DBC execute non-blocking database transactions, and how does its pool differ from HikariCP?
 */
@SuppressWarnings({"UnusedVariable", "unused"})
public class Q27R2dbcConnectionPoolingAndTransactionsExample {

    public static void main(String[] args) {
        // R2DBC (Reactive Relational Database Connectivity) vs JDBC:
        // - JDBC / HikariCP: Synchronous blocking sockets. Each thread occupies a dedicated database connection.
        //   HikariCP manages blocking threads via Semaphore / LockSupport.
        // - R2DBC Pool (r2dbc-pool): Built on Netty asynchronous socket event loops. A connection is represented
        //   as a Mono<Connection>. Threads do not block during socket queries.
        //
        // Transaction Management:
        // Handled via TransactionalOperator or R2dbcTransactionManager.
        // Binds the active reactive Connection to the subscriber's Reactor Context, ensuring all
        // statements chained within the transaction flow execute on that specific acquired connection.

        Map<String, String> poolComparison =
                Map.of(
                        "HikariCP", "Blocking JDBC; thread-per-connection model; synchronized handoff",
                        "R2DBC Pool", "Non-blocking reactive sockets; connection bound via Reactor Context");

        boolean r2dbcBindsConnectionViaContext =
                poolComparison.get("R2DBC Pool").contains("Reactor Context"); // true
    }
}
