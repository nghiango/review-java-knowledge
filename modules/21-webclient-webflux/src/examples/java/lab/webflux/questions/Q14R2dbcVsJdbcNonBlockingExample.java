package lab.webflux.questions;

public class Q14R2dbcVsJdbcNonBlockingExample {

    record DatabaseDriverModel(String protocol, boolean fullyNonBlocking, boolean supportsJpaOrm) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // JDBC is inherently blocking: threads block on SocketInputStream.read() waiting for
        // database packets.
        // R2DBC (Reactive Relational Database Connectivity) is fully non-blocking and event-driven
        // over Netty.
        // However, R2DBC does not support JPA/Hibernate (entity graph navigation, lazy loading,
        // dirty checking).
        DatabaseDriverModel jdbc = new DatabaseDriverModel("JDBC", false, true);
        DatabaseDriverModel r2dbc = new DatabaseDriverModel("R2DBC", true, false);

        boolean r2dbcIsNonBlocking = r2dbc.fullyNonBlocking(); // true
        boolean jpaSupportedByR2dbc = r2dbc.supportsJpaOrm(); // false

        System.out.println("R2DBC is fully non-blocking: " + r2dbcIsNonBlocking);
        System.out.println("R2DBC supports JPA/Hibernate ORM: " + jpaSupportedByR2dbc);
    }
}
