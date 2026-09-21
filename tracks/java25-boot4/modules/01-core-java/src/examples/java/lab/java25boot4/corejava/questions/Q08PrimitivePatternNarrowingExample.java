package lab.java25boot4.corejava.questions;

public class Q08PrimitivePatternNarrowingExample {

    public static int toSmallInt(Object value) {
        if (value instanceof int i) {
            // The cast is unchecked: values outside byte range wrap silently.
            return (byte) i;
        }
        return -1;
    }

    public static void main(String[] args) {
        System.out.println(toSmallInt(100)); // 100
        System.out.println(toSmallInt(300)); // 44 — 300 wrapped into a byte, no exception
        System.out.println((byte) 300); // 44
    }
}
