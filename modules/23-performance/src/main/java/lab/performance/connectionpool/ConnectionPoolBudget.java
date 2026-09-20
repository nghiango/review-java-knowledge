package lab.performance.connectionpool;

/** Computes a per-instance pool cap from a database-wide connection budget. */
public record ConnectionPoolBudget(int databaseLimit, int reservedConnections, int instances) {

    public ConnectionPoolBudget {
        if (databaseLimit <= 0 || instances <= 0) {
            throw new IllegalArgumentException("databaseLimit and instances must be positive");
        }
        if (reservedConnections < 0 || reservedConnections >= databaseLimit) {
            throw new IllegalArgumentException(
                    "reservedConnections must leave application capacity");
        }
    }

    public int maximumPoolSize() {
        // One connection keeps a small installation usable; larger deployments divide the budget.
        return Math.max(1, (databaseLimit - reservedConnections) / instances);
    }
}
