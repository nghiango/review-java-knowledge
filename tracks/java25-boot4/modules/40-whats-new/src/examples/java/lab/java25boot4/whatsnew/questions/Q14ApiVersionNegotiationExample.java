package lab.java25boot4.whatsnew.questions;

/** Q14: what is first-class API versioning in Spring MVC / WebFlux? */
public class Q14ApiVersionNegotiationExample {

    // Boot 4 resolves the request version and matches it against the handler's `version` attribute.
    // This models the matching rule; the framework supplies the resolver.
    static boolean matches(String requestedVersion, String handlerVersion) {
        return requestedVersion != null && requestedVersion.equals(handlerVersion);
    }

    public static void main(String[] args) {
        System.out.println(matches("1.2", "1.2")); // true — handler for v1.2 is selected
        System.out.println(matches("2.0", "1.2")); // false — no matching handler, framework negotiates
        System.out.println(matches(null, "1.2")); // false — no version requested
    }
}
