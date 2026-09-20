package lab.jpahibernate.questions;

import jakarta.persistence.InheritanceType;

public class Q19InheritanceMappingStrategies {

    public static void main(String[] args) {
        // SINGLE_TABLE (Default): High query performance (no joins), but nullable subclass columns
        // & wide table
        InheritanceType singleTable = InheritanceType.SINGLE_TABLE;
        // JOINED: Normalized schema, non-null constraints supported, but polymorphic queries
        // require multi-table OUTER JOINs
        InheritanceType joined = InheritanceType.JOINED;
        // TABLE_PER_CLASS: Each concrete class has its own table, polymorphic queries require UNION
        // ALL
        InheritanceType tablePerClass = InheritanceType.TABLE_PER_CLASS;

        boolean singleTableRequiresNoJoins = (singleTable == InheritanceType.SINGLE_TABLE); // true
        boolean joinedRequiresOuterJoinsForPolymorphism =
                (joined == InheritanceType.JOINED); // true
        boolean tablePerClassRequiresUnionQueries =
                (tablePerClass == InheritanceType.TABLE_PER_CLASS); // true

        System.out.println(
                "SINGLE_TABLE no joins required: "
                        + singleTableRequiresNoJoins); // SINGLE_TABLE no joins required: true
        System.out.println(
                "JOINED requires outer joins: "
                        + joinedRequiresOuterJoinsForPolymorphism); // JOINED requires outer joins:
        // true
        System.out.println(
                "TABLE_PER_CLASS requires UNION: "
                        + tablePerClassRequiresUnionQueries); // TABLE_PER_CLASS requires UNION:
        // true
    }
}
