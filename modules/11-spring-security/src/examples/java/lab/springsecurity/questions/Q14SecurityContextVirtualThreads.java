package lab.springsecurity.questions;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class Q14SecurityContextVirtualThreads {

    public static void main(String[] args) throws Exception {
        // Virtual threads and Spring Security context propagation
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken("alice_vt", null, java.util.List.of()));
        SecurityContextHolder.setContext(context);

        // Explicit SecurityContext propagation across asynchronous boundaries
        SecurityContext capturedContext = SecurityContextHolder.getContext();

        Thread vt =
                Thread.ofVirtual()
                        .start(
                                () -> {
                                    SecurityContextHolder.setContext(capturedContext);
                                    try {
                                        String user =
                                                SecurityContextHolder.getContext()
                                                        .getAuthentication()
                                                        .getName();
                                        System.out.println(
                                                "Principal inside Virtual Thread: "
                                                        + user); // Principal inside Virtual Thread:
                                        // alice_vt
                                    } finally {
                                        SecurityContextHolder.clearContext();
                                    }
                                });
        vt.join();

        SecurityContextHolder.clearContext();
        boolean isClean = (SecurityContextHolder.getContext().getAuthentication() == null); // true
        System.out.println("Parent thread cleared: " + isClean); // Parent thread cleared: true
    }
}
