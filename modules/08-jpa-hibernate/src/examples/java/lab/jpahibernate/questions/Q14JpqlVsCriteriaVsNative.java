package lab.jpahibernate.questions;

public class Q14JpqlVsCriteriaVsNative {

    public static void main(String[] args) {
        // JPQL: Database-agnostic, entity-centric, checked at runtime query execution
        boolean jpqlIsTypeSafeAtCompileTime = false; // false
        // Criteria API: Type-safe via Metamodel, dynamic query construction, verbose
        boolean criteriaApiIsTypeSafeAtCompileTime = true; // true
        // Native SQL: Direct database dialect access, bypasses entity abstraction if not mapped
        boolean nativeSqlBypassesOrmPortability = true; // true

        System.out.println(
                "JPQL compile-time type-safe: "
                        + jpqlIsTypeSafeAtCompileTime); // JPQL compile-time type-safe: false
        System.out.println(
                "Criteria compile-time type-safe: "
                        + criteriaApiIsTypeSafeAtCompileTime); // Criteria compile-time type-safe:
        // true
        System.out.println(
                "Native SQL bypasses portability: "
                        + nativeSqlBypassesOrmPortability); // Native SQL bypasses portability: true
    }
}
