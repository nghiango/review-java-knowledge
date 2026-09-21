package lab.designpatterns.questions;

/**
 * Q15: Visitor Pattern & Double Dispatch. Demonstrates adding operations to an object structure
 * without modifying the element classes.
 */
public class Q15VisitorPatternDoubleDispatchExample {

    public interface Element {
        String accept(Visitor visitor);
    }

    public static class TextElement implements Element {
        private final String content;

        public TextElement(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String accept(Visitor visitor) {
            return visitor.visitText(this);
        }
    }

    public interface Visitor {
        String visitText(TextElement text);
    }

    public static class HtmlVisitor implements Visitor {
        @Override
        public String visitText(TextElement text) {
            return "<p>" + text.getContent() + "</p>";
        }
    }

    public static void main(String[] args) {
        Element el = new TextElement("Design Patterns");
        Visitor htmlVisitor = new HtmlVisitor();

        String html = el.accept(htmlVisitor); // "<p>Design Patterns</p>"
        boolean doubleDispatched = html.equals("<p>Design Patterns</p>"); // true

        System.out.println("Q15 html: " + html + ", doubleDispatched: " + doubleDispatched);
    }
}
