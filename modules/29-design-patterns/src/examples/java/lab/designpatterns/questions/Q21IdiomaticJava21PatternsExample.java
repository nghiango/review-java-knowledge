package lab.designpatterns.questions;

/**
 * Q21: Modern Java 21 Functional & Pattern-Matching Alternatives to Gang of Four Patterns.
 * Demonstrates sealed hierarchies and pattern-matching switch replacing Visitor pattern.
 */
public class Q21IdiomaticJava21PatternsExample {

    public sealed interface Expression permits Constant, Add, Multiply {}

    public record Constant(int val) implements Expression {}

    public record Add(Expression left, Expression right) implements Expression {}

    public record Multiply(Expression left, Expression right) implements Expression {}

    // Pattern matching switch replaces the entire Visitor interface & double dispatch
    public static int evaluate(Expression expr) {
        return switch (expr) {
            case Constant c -> c.val();
            case Add a -> evaluate(a.left()) + evaluate(a.right());
            case Multiply m -> evaluate(m.left()) * evaluate(m.right());
        };
    }

    public static void main(String[] args) {
        // (3 + 5) * 2 = 16
        Expression expr = new Multiply(new Add(new Constant(3), new Constant(5)), new Constant(2));
        int result = evaluate(expr); // 16

        boolean evaluatedCorrectly = (result == 16); // true

        System.out.println("Q21 result: " + result + ", correct: " + evaluatedCorrectly);
    }
}
