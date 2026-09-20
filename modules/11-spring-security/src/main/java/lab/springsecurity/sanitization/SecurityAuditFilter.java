package lab.springsecurity.sanitization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

public class SecurityAuditFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(SecurityAuditFilter.class.getName());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            String maskedHeader = maskAuthorizationHeader(authHeader);
            LOGGER.info(
                    "Audit request to "
                            + request.getRequestURI()
                            + " [Auth: "
                            + maskedHeader
                            + "]");
        }

        filterChain.doFilter(request, response);
    }

    public static String maskAuthorizationHeader(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return "[EMPTY]";
        }
        if (authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (token.length() <= 8) {
                return "Bearer ***";
            }
            return "Bearer " + token.substring(0, 4) + "..." + token.substring(token.length() - 4);
        }
        if (authHeader.startsWith("Basic ")) {
            return "Basic [PROTECTED]";
        }
        return "[REDACTED]";
    }
}
