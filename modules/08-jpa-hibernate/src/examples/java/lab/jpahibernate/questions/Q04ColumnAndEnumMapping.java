package lab.jpahibernate.questions;

import jakarta.persistence.EnumType;

public class Q04ColumnAndEnumMapping {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    @SuppressWarnings("EnumOrdinal")
    public static void main(String[] args) {
        // EnumType.ORDINAL persists 0, 1, 2 (breaks if enum constants are reordered or inserted)
        EnumType ordinalMapping = EnumType.ORDINAL;
        // EnumType.STRING persists "PENDING", "APPROVED", "REJECTED" (safe against reordering)
        EnumType stringMapping = EnumType.STRING;

        int pendingOrdinal = Status.PENDING.ordinal(); // 0
        String approvedString = Status.APPROVED.name(); // "APPROVED"

        boolean ordinalFragileOnReorder = (ordinalMapping == EnumType.ORDINAL); // true
        boolean stringSafeOnReorder = (stringMapping == EnumType.STRING); // true

        System.out.println("Ordinal value: " + pendingOrdinal); // Ordinal value: 0
        System.out.println("String value: " + approvedString); // String value: APPROVED
        System.out.println(
                "Ordinal fragile on reorder: "
                        + ordinalFragileOnReorder); // Ordinal fragile on reorder: true
        System.out.println(
                "String safe on reorder: " + stringSafeOnReorder); // String safe on reorder: true
    }
}
