package lab.springboot.questions;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

public class Q14ActuatorHealthGroupsProbesExample {

    static class CustomDatabaseHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            return Health.up()
                    .withDetail("database", "PostgreSQL 16")
                    .withDetail("activeConnections", 5)
                    .build();
        }
    }

    public static void main(String[] args) {
        CustomDatabaseHealthIndicator indicator = new CustomDatabaseHealthIndicator();
        Health health = indicator.health();

        Status status = health.getStatus(); // UP
        boolean isUp = Status.UP.equals(status); // true
        Object dbVersion = health.getDetails().get("database"); // "PostgreSQL 16"

        System.out.println(
                "Health status: " + status + ", database: " + dbVersion + ", isUp: " + isUp);
    }
}
