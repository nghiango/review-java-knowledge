package lab.corejava.questions;

@SuppressWarnings({"unused", "ReferenceEquality"})
public final class Q01EqualsVsIdentityExample {
    private Q01EqualsVsIdentityExample() {}

    public static void main(String[] args) {
        String a = new String("order-42");
        String b = new String("order-42");

        boolean referenceEquality = (a == b); // false (different memory addresses on heap)
        boolean logicalEquality = a.equals(b); // true (String overrides equals to compare chars)

        int x = 100;
        int y = 100;
        boolean primitiveEquality = (x == y); // true (compares raw primitive values)
    }
}
