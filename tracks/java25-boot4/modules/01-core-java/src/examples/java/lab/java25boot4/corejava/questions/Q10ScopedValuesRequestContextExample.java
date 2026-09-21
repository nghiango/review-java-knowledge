package lab.java25boot4.corejava.questions;

public class Q10ScopedValuesRequestContextExample {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    public static void main(String[] args) {
        ScopedValue.where(REQUEST_ID, "req-42").run(() -> System.out.println(REQUEST_ID.get()));
        // "req-42"

        System.out.println(REQUEST_ID.isBound()); // false — the binding ended with run()
    }
}
