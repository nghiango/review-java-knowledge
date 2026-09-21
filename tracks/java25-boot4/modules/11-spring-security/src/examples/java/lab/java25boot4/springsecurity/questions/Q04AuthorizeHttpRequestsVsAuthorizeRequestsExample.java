package lab.java25boot4.springsecurity.questions;

/**
 * Q04: What are the differences between authorizeHttpRequests and the obsolete authorizeRequests in
 * Spring Security?
 */
public class Q04AuthorizeHttpRequestsVsAuthorizeRequestsExample {

    public record AuthorizationDifference(
            String aspect, String authorizeRequests, String authorizeHttpRequests) {}

    public static void main(String[] args) {
        var diff =
                new AuthorizationDifference(
                        "Underlying Engine",
                        "AccessDecisionManager / ConfigAttribute (Legacy)",
                        "AuthorizationManager (Modern Spring 6/7)");

        System.out.println("Aspect: " + diff.aspect()); // "Underlying Engine"
        System.out.println(
                "Modern: " + diff.authorizeHttpRequests()); // "AuthorizationManager (Modern Spring
        // 6/7)"
        System.out.println("Supports lambda DSL: " + true); // true
    }
}
