package lab.jpahibernate.questions;

public class Q16OpenSessionInViewPitfalls {

    public static void main(String[] args) {
        // Open Session in View (OSIV) binds an EntityManager/Session to the entire HTTP request
        // thread lifecycle.
        // Spring Boot default: spring.jpa.open-in-view=true (emits WARN in logs)
        boolean osivEnabledByDefaultInSpringBoot = true; // true

        // Drawbacks of OSIV in high-throughput production systems:
        // 1. Database connections held open during slow template rendering or network I/O ->
        // connection pool exhaustion.
        // 2. Unintended N+1 queries during JSON serialization.
        // 3. Blurs architecture boundaries between presentation and service layers.
        boolean recommendedForProductionHighThroughput = false; // false

        System.out.println(
                "OSIV default in Spring Boot: "
                        + osivEnabledByDefaultInSpringBoot); // OSIV default in Spring Boot: true
        System.out.println(
                "OSIV recommended in high-throughput production: "
                        + recommendedForProductionHighThroughput); // OSIV recommended in
        // high-throughput production:
        // false
    }
}
