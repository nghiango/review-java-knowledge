package lab.java25boot4.whatsnew.questions;

/** Q4: what happens to the {@code SecurityManager} on Java 25? */
public class Q04SecurityManagerDisabledExample {

    public static void main(String[] args) {
        // Java 25 permanently disables the SecurityManager (JEP 486): it can never be installed.
        SecurityManager securityManager = System.getSecurityManager();

        System.out.println(securityManager); // null — there is no way to enable it
        System.out.println(System.getProperty("java.security.manager")); // null
    }
}
