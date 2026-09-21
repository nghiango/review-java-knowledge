package lab.designpatterns.questions;

/**
 * Q05: Decorator vs Proxy Pattern. Demonstrates intent difference: Decorator enhances behavior
 * dynamically, while Proxy controls access / lifecycle.
 */
public class Q05DecoratorVsProxyExample {

    public interface TextService {
        String getText();
    }

    public static class SimpleTextService implements TextService {
        @Override
        public String getText() {
            return "hello world";
        }
    }

    // Decorator: Enhances/adds behavior (appends prefix/suffix)
    public static class UpperCaseDecorator implements TextService {
        private final TextService delegate;

        public UpperCaseDecorator(TextService delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getText() {
            return delegate.getText().toUpperCase(java.util.Locale.ROOT);
        }
    }

    // Proxy: Controls access (lazy initialization / permissions)
    public static class LazyAccessProxy implements TextService {
        private TextService realService;

        @Override
        public String getText() {
            if (realService == null) {
                realService = new SimpleTextService();
            }
            return realService.getText();
        }
    }

    public static void main(String[] args) {
        TextService decorator = new UpperCaseDecorator(new SimpleTextService());
        String decorated = decorator.getText(); // "HELLO WORLD"

        TextService proxy = new LazyAccessProxy();
        String proxied = proxy.getText(); // "hello world"

        boolean decoratorEnhanced = decorated.equals("HELLO WORLD"); // true
        boolean proxyControlled = proxied.equals("hello world"); // true

        System.out.println("Q05 decorator: " + decoratorEnhanced + ", proxy: " + proxyControlled);
    }
}
