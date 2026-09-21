package lab.java25boot4.corejava.questions;

public class Q11GuardedPrimitivePatternExample {

    public static int toSmallInt(Object value) {
        return switch (value) {
            case Integer i when i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE -> i;
            case Integer i -> throw new IllegalArgumentException("out of byte range: " + i);
            default -> -1;
        };
    }

    public static void main(String[] args) {
        System.out.println(toSmallInt(100)); // 100
        try {
            toSmallInt(300);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage()); // "out of byte range: 300"
        }
    }
}
