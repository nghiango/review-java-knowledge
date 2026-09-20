package lab.jpahibernate.questions;

public class Q17DynamicUpdateAndBytecodeEnhancement {

    public static void main(String[] args) {
        // By default, Hibernate generates cached static UPDATE statements containing all entity
        // columns:
        // "UPDATE table SET col1=?, col2=?, col3=?, col4=? WHERE id=?"
        boolean defaultUpdateIncludesAllColumns = true; // true

        // @DynamicUpdate generates dynamic SQL at runtime with only modified columns:
        // "UPDATE table SET col2=? WHERE id=?"
        // Trade-off: Prevents statement cache reuse in DB & incurs CPU overhead calculating dynamic
        // SQL strings.
        boolean dynamicUpdateDisablesPrecompiledStatementReuse = true; // true

        System.out.println(
                "Default UPDATE includes all columns: "
                        + defaultUpdateIncludesAllColumns); // Default UPDATE includes all columns:
        // true
        System.out.println(
                "DynamicUpdate disables precompiled SQL cache: "
                        + dynamicUpdateDisablesPrecompiledStatementReuse); // DynamicUpdate disables
        // precompiled SQL cache:
        // true
    }
}
