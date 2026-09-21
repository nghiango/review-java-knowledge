package lab.java25boot4.concurrency.questions;

public class Q06ScopedValueRebindingExample {

    private static final ScopedValue<String> ROLE = ScopedValue.newInstance();

    public static void main(String[] args) {
        ScopedValue.where(ROLE, "ANONYMOUS")
                .run(
                        () -> {
                            System.out.println(ROLE.get()); // ANONYMOUS

                            // Nested rebinding creates an inner frame without mutating outer frame
                            ScopedValue.where(ROLE, "ELEVATED_ADMIN")
                                    .run(
                                            () -> {
                                                System.out.println(ROLE.get()); // ELEVATED_ADMIN
                                            });

                            System.out.println(ROLE.get()); // ANONYMOUS
                        });
    }
}
