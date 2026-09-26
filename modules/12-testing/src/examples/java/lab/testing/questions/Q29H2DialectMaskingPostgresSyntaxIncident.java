package lab.testing.questions;

import java.util.Map;

/**
 * Q29: Production Incident: H2 in-memory test database passes, but production fails with
 * PostgreSQL syntax error. How do you triage and resolve?
 */
public class Q29H2DialectMaskingPostgresSyntaxIncident {

    public static void main(String[] args) {
        // Native PostgreSQL query utilizing JSONB containment and ON CONFLICT upsert
        String postgresQuery =
                "INSERT INTO user_profiles (id, metadata) VALUES (:id, CAST(:meta AS jsonb)) "
                        + "ON CONFLICT (id) DO UPDATE SET metadata = EXCLUDED.metadata "
                        + "WHERE user_profiles.metadata ->> 'version' = '2'";

        // H2 in MODE=PostgreSQL: parses basic syntax but silently accepts invalid constraints
        // and fails or behaves differently on native JSONB operators (->, ->>, ?|)
        boolean usesPostgresJsonbOperator = postgresQuery.contains("->>"); // true
        boolean usesDoUpdateExcluded = postgresQuery.contains("EXCLUDED."); // true

        // Resolution: Banish H2 for persistence testing; use Testcontainers PostgreSQL
        Map<String, String> testcontainersConfig =
                Map.of(
                        "spring.datasource.url", "jdbc:tc:postgresql:16:///testdb",
                        "spring.datasource.driver-class-name", "org.testcontainers.jdbc.ContainerDatabaseDriver");

        boolean enforcesRealPostgres =
                testcontainersConfig.get("spring.datasource.url").contains("tc:postgresql"); // true

        System.out.println("Query Uses Native JSONB Operator: " + usesPostgresJsonbOperator); // true
        System.out.println("Query Uses EXCLUDED Upsert: " + usesDoUpdateExcluded); // true
        System.out.println("Testcontainers Enforces Production Engine: " + enforcesRealPostgres); // true
    }
}
