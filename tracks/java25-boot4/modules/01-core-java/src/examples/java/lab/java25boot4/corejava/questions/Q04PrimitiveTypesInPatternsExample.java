package lab.java25boot4.corejava.questions;

public class Q04PrimitiveTypesInPatternsExample {

    public static String classifyNumber(Object obj) {
        return switch (obj) {
            case byte b -> "byte: " + b;
            case short s -> "short: " + s;
            case int i when i > 0 -> "positive int: " + i;
            case int i -> "non-positive int: " + i;
            case long l -> "long: " + l;
            case double d -> "double: " + d;
            case null -> "null value";
            default -> "other: " + obj;
        };
    }

    public static void main(String[] args) {
        System.out.println(classifyNumber((byte) 5)); // "byte: 5"
        System.out.println(classifyNumber(42)); // "positive int: 42"
        System.out.println(classifyNumber(-7)); // "non-positive int: -7"
        System.out.println(classifyNumber(100L)); // "long: 100"
    }
}
