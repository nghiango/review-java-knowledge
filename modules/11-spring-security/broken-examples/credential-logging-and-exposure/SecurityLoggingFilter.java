package lab.springsecurity.broken.sanitization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

public class SecurityLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(SecurityLoggingFilter.class.getName());

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Logs sensitive Authorization header (JWT / API Key / Basic Auth credentials) unmasked
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            LOGGER.info("Incoming request to " + request.getRequestURI() + " with Authorization: " + authHeader);
        }

        filterChain.doFilter(request, response);
    }
}
