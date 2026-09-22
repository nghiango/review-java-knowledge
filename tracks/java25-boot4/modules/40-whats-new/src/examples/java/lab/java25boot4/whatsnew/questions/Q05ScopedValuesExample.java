package lab.java25boot4.whatsnew.questions;

/** Q5: what are scoped values? */
public class Q05ScopedValuesExample {

    private static final ScopedValue<String> TENANT = ScopedValue.newInstance();

    public static void main(String[] args) {
        ScopedValue.where(TENANT, "acme").run(Q05ScopedValuesExample::handle);

        System.out.println(TENANT.isBound()); // false — the binding ended with the scope
    }

    private static void handle() {
        System.out.println(TENANT.get()); // acme — visible for the dynamic extent of the scope
    }
}
