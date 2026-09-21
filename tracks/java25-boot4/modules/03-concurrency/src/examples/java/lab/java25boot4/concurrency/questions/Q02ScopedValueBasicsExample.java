package lab.java25boot4.concurrency.questions;

public class Q02ScopedValueBasicsExample {

    private static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    public static void main(String[] args) {
        System.out.println(CORRELATION_ID.isBound()); // false

        ScopedValue.where(CORRELATION_ID, "req-12345")
                .run(
                        () -> {
                            System.out.println(CORRELATION_ID.isBound()); // true
                            System.out.println(CORRELATION_ID.get()); // req-12345
                        });

        System.out.println(CORRELATION_ID.isBound()); // false
    }
}
