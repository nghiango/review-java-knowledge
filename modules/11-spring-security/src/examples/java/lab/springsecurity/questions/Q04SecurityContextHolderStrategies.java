package lab.springsecurity.questions;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class Q04SecurityContextHolderStrategies {

    public static void main(String[] args) {
        // Default Strategy: MODE_THREADLOCAL
        String defaultStrategy = SecurityContextHolder.MODE_THREADLOCAL; // "MODE_THREADLOCAL"

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
        SecurityContextHolder.setContext(context);

        String currentPrincipal =
                SecurityContextHolder.getContext().getAuthentication().getName(); // "alice"

        // Always clear context at request completion / thread boundary
        SecurityContextHolder.clearContext();
        boolean isCleared =
                (SecurityContextHolder.getContext().getAuthentication() == null); // true

        System.out.println(
                "Default Holder Strategy: "
                        + defaultStrategy); // Default Holder Strategy: MODE_THREADLOCAL
        System.out.println(
                "Authenticated Principal: " + currentPrincipal); // Authenticated Principal: alice
        System.out.println("Context Cleared: " + isCleared); // Context Cleared: true
    }
}
