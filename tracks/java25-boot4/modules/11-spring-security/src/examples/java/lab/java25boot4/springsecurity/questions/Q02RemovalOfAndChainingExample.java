package lab.java25boot4.springsecurity.questions;

import java.util.List;

/**
 * Q02: Why was the and() method chaining removed in Spring Security 7, and how does nested lambda
 * scoping prevent configuration bugs?
 */
public class Q02RemovalOfAndChainingExample {

    public record SecurityRule(String pattern, String role, boolean permitted) {}

    public static void main(String[] args) {
        // Nested lambda blocks guarantee explicit lexical scoping without context bleeding
        List<SecurityRule> rules =
                List.of(
                        new SecurityRule("/public/**", "ANONYMOUS", true),
                        new SecurityRule("/admin/**", "ROLE_ADMIN", false));

        boolean publicPermitted = rules.get(0).permitted();
        boolean adminProtected = !rules.get(1).permitted();

        System.out.println("Public permitted: " + publicPermitted); // true
        System.out.println("Admin protected: " + adminProtected); // true
        System.out.println("Rule count: " + rules.size()); // 2
    }
}
