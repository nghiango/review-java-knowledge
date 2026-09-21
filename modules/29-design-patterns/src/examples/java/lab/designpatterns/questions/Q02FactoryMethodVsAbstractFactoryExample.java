package lab.designpatterns.questions;

/**
 * Q02: Factory Method vs Abstract Factory Pattern. Demonstrates a single Factory Method producing a
 * product vs an Abstract Factory producing families of related products.
 */
public class Q02FactoryMethodVsAbstractFactoryExample {

    // 1. Products
    public interface Button {
        String render();
    }

    public interface Checkbox {
        String render();
    }

    public static class DarkButton implements Button {
        @Override
        public String render() {
            return "DarkButton";
        }
    }

    public static class DarkCheckbox implements Checkbox {
        @Override
        public String render() {
            return "DarkCheckbox";
        }
    }

    // 2. Abstract Factory: creates families of related objects
    public interface GuiThemeFactory {
        Button createButton();

        Checkbox createCheckbox();
    }

    public static class DarkThemeFactory implements GuiThemeFactory {
        @Override
        public Button createButton() {
            return new DarkButton();
        }

        @Override
        public Checkbox createCheckbox() {
            return new DarkCheckbox();
        }
    }

    public static void main(String[] args) {
        GuiThemeFactory factory = new DarkThemeFactory();
        Button btn = factory.createButton();
        Checkbox chk = factory.createCheckbox();

        boolean isDarkFamily =
                btn.render().equals("DarkButton") && chk.render().equals("DarkCheckbox"); // true

        System.out.println("Q02 abstractFactory: " + isDarkFamily);
    }
}
