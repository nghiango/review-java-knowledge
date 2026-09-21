package lab.java25boot4.corejava.questions;

public class Q07PrimitivePatternNullSafetyExample {

    public static String describe(Object value) {
        return switch (value) {
            case byte b -> "byte: " + b;
            case int i -> "int: " + i;
            case long l -> "long: " + l;
            case double d -> "double: " + d;
            // A primitive pattern never matches null, so this branch is mandatory.
            case null -> "null";
            default -> "other: " + value;
        };
    }

    public static void main(String[] args) {
        System.out.println(describe(null)); // "null"
        System.out.println(describe(7)); // "int: 7"
        System.out.println(describe((short) 7)); // "other: 7" — a Short does not match the int pattern
    }
}
