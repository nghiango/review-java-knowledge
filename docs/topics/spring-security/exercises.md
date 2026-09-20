# Spring Security Exercises

Hands-on exercises to practice custom SpEL security evaluators, dynamic permission beans, and rate-limiting security filters.

## Exercise 1: Build a Custom SpEL Security Evaluator for Multi-Tenant Access

Implement a Spring `@Component` bean that evaluates whether the currently authenticated principal is authorized to perform operations within a specified tenant organization. Wire this bean into `@PreAuthorize`.

### Requirements
- Create `@Component("tenantSecurity")`.
- Expose method `public boolean canAccessTenant(Authentication authentication, String tenantId)`.
- Allow access if the user has `ROLE_SUPER_ADMIN` or if the user's tenant claim matches the requested `tenantId`.
- Use the expression `@PreAuthorize("@tenantSecurity.canAccessTenant(authentication, #tenantId)")` on a tenant service method.

??? question "Reveal solution"
    ```java
    @Component("tenantSecurity")
    public class TenantSecurityEvaluator {

        public boolean canAccessTenant(Authentication authentication, String tenantId) {
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> auth.equals("ROLE_SUPER_ADMIN"));

            if (isSuperAdmin) {
                return true;
            }

            if (authentication.getPrincipal() instanceof CustomTenantPrincipal principal) {
                return principal.tenantId().equalsIgnoreCase(tenantId);
            }

            return false;
        }
    }

    public record CustomTenantPrincipal(String username, String tenantId) {}

    @Service
    public class TenantDataService {

        @PreAuthorize("@tenantSecurity.canAccessTenant(authentication, #tenantId)")
        public String fetchTenantData(String tenantId) {
            return "Sensitive Data for Tenant " + tenantId;
        }
    }
    ```

---

## Exercise 2: Implement a Rate-Limiting Authentication Guard Filter

Implement a custom `OncePerRequestFilter` that intercepts login POST requests to `/api/auth/login` and enforces IP-based rate limiting (maximum 5 attempts per minute per IP address) before delegating to downstream filters.

### Requirements
- Extend `OncePerRequestFilter`.
- Apply rate limiting only to `POST /api/auth/login`.
- Extract client IP from `request.getRemoteAddr()`.
- Return HTTP `429 Too Many Requests` with a ProblemDetail body if the threshold is exceeded.
- Wire the filter before `UsernamePasswordAuthenticationFilter` in the filter chain.

??? question "Reveal solution"
    ```java
    @Component
    public class LoginRateLimiterFilter extends OncePerRequestFilter {

        private final Map<String, TokenBucket> ipBuckets = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {

            if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/auth/login".equals(request.getRequestURI())) {
                String clientIp = request.getRemoteAddr();
                TokenBucket bucket = ipBuckets.computeIfAbsent(clientIp, k -> new TokenBucket(5, Duration.ofMinutes(1)));

                if (!bucket.tryConsume()) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.getWriter().write("""
                            {
                              "type": "https://api.example.com/errors/rate-limit",
                              "title": "Too Many Requests",
                              "status": 429,
                              "detail": "Maximum authentication attempts exceeded. Please try again later."
                            }
                            """);
                    return;
                }
            }

            filterChain.doFilter(request, response);
        }

        static class TokenBucket {
            private final int capacity;
            private final Duration refillPeriod;
            private int tokens;
            private Instant lastRefill;

            TokenBucket(int capacity, Duration refillPeriod) {
                this.capacity = capacity;
                this.tokens = capacity;
                this.refillPeriod = refillPeriod;
                this.lastRefill = Instant.now();
            }

            synchronized boolean tryConsume() {
                Instant now = Instant.now();
                if (Duration.between(lastRefill, now).compareTo(refillPeriod) >= 0) {
                    tokens = capacity;
                    lastRefill = now;
                }
                if (tokens > 0) {
                    tokens--;
                    return true;
                }
                return false;
            }
        }
    }
    ```
